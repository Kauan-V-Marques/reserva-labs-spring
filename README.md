# Reserva de Laboratórios

![CI](https://github.com/Kauan-V-Marques/reserva-labs-spring/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)

API REST para **reservar laboratórios da faculdade** (eletrônica, robótica, informática...). Ela mostra os horários livres de cada dia, impede reservas que se sobrepõem e aplica as regras de uso de cada laboratório.

## Regras de negócio

- Horários em blocos de **15 minutos**, com duração entre **30 minutos e 4 horas**, no mesmo dia
- Só dentro do **horário de funcionamento** do laboratório
- Não aceita reserva no passado, nem com mais de 60 dias de antecedência
- O número de participantes não pode passar da **capacidade** do laboratório
- **Duas reservas confirmadas nunca se sobrepõem.** Começar exatamente quando a outra termina é permitido
- Só **quem fez a reserva** pode cancelar, e apenas antes de ela começar. O horário volta a ficar livre

### Sobreposição protegida em duas camadas

1. **Na aplicação:** o laboratório é travado com `SELECT ... FOR UPDATE` (lock pessimista) antes da verificação. Assim, dois pedidos simultâneos para o mesmo laboratório esperam a vez em vez de passarem juntos.
2. **No PostgreSQL:** uma *exclusion constraint* com `tsrange` recusa qualquer sobreposição, mesmo que alguém grave direto no banco:

```sql
ALTER TABLE reservations
    ADD CONSTRAINT ex_reservations_no_overlap
    EXCLUDE USING gist (laboratory_id WITH =, tsrange(starts_at, ends_at) WITH &&)
    WHERE (status = 'CONFIRMED');
```

## Tecnologias

| | |
|---|---|
| Linguagem | Java 25 (records, text blocks) |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Banco | PostgreSQL 18, com migrations Flyway |
| Testes | JUnit 5, AssertJ, MockMvc. H2 localmente e PostgreSQL real no CI |
| Infra | Docker, Docker Compose e GitHub Actions |

Outros detalhes da implementação:
- Os erros seguem o padrão **Problem Details** (RFC 9457): 400 com a lista de campos inválidos, 403, 404, 409 e 422
- O "agora" vem de um `Clock` injetado, o que deixa os testes determinísticos
- A busca de reservas usa **Specifications** do Spring Data para os filtros opcionais
- A pasta `db/migration/{vendor}` guarda scripts exclusivos do PostgreSQL

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/labs` | Laboratórios ativos |
| GET | `/api/labs/{id}` | Detalhe do laboratório |
| POST / PUT | `/api/labs`, `/api/labs/{id}` | Cadastra / atualiza |
| GET | `/api/labs/{id}/availability?date=2026-11-20` | Horários reservados e livres do dia |
| POST | `/api/reservations` | Faz uma reserva |
| GET | `/api/reservations?laboratoryId=&date=&email=&status=` | Busca com filtros opcionais |
| GET | `/api/reservations/{id}` | Detalhe da reserva |
| POST | `/api/reservations/{id}/cancel` | Cancela (informando o e-mail de quem reservou) |
| GET | `/actuator/health` | Saúde da aplicação |

Exemplo de disponibilidade:

```json
{
  "laboratory": { "id": 2, "code": "ROB-01", "name": "Laboratorio de Robotica" },
  "date": "2026-11-20",
  "opensAt": "08:00:00",
  "closesAt": "22:00:00",
  "reserved": [ { "start": "14:00:00", "end": "16:00:00", "minutes": 120 } ],
  "free": [
    { "start": "08:00:00", "end": "14:00:00", "minutes": 360 },
    { "start": "16:00:00", "end": "22:00:00", "minutes": 360 }
  ]
}
```

Há mais exemplos prontos para usar no VS Code (REST Client) ou no IntelliJ em [`exemplos.http`](exemplos.http).

## Como rodar

**Tudo com Docker** (API + PostgreSQL):

```bash
docker compose up --build
```

A API sobe em http://localhost:8080, já com 5 laboratórios de exemplo.

**Pelo Maven** (com o PostgreSQL do compose):

```bash
docker compose up -d db
./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test
```

São 26 testes:
- **Unitários** das regras de reserva (cada período inválido é um caso parametrizado) e do cálculo de horários livres
- **De integração**, de ponta a ponta com MockMvc: criação, conflito e reservas encostadas, regras 422, validação 400, permissão para cancelar, disponibilidade e filtros

Localmente os testes usam H2 em modo PostgreSQL. No GitHub Actions, as mesmas classes rodam contra um **PostgreSQL 18 real**, incluindo a exclusion constraint.

---

<details>
<summary>English</summary>

### Lab Booking API

A **Java 25 / Spring Boot 4** REST API to book university labs, backed by **PostgreSQL** and Flyway migrations.

- 15-minute slots, 30 min to 4 h, within opening hours, no past bookings, up to 60 days ahead, capacity limits
- No overlapping confirmed bookings, enforced twice: a pessimistic lock in the service and a PostgreSQL `EXCLUDE USING gist` constraint on `tsrange`
- Daily availability (free and booked slots), requester-only cancellation, RFC 9457 Problem Details errors
- 26 unit and MockMvc integration tests; H2 locally and a real PostgreSQL 18 in GitHub Actions; Docker Compose

Run: `docker compose up --build`, then call http://localhost:8080/api/labs.

</details>
