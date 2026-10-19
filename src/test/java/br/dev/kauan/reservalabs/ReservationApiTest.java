package br.dev.kauan.reservalabs;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de ponta a ponta da API (HTTP -> serviço -> JPA -> banco).
 * Localmente usam H2; no CI rodam contra um PostgreSQL real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ReservationApiTest.FixedClock.class)
class ReservationApiTest {

    /** "Agora" fixo: segunda-feira, 16/11/2026, 08:00. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            ZoneId zone = ZoneId.of("America/Sao_Paulo");
            return Clock.fixed(LocalDateTime.of(2026, 11, 16, 8, 0).atZone(zone).toInstant(), zone);
        }
    }

    @Autowired
    MockMvc mvc;

    long labId;

    @BeforeEach
    void createLab() throws Exception {
        String body = mvc.perform(post("/api/labs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "tst-01", "name": "Lab de Testes", "building": "Bloco T",
                         "capacity": 20, "opensAt": "08:00", "closesAt": "18:00"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TST-01"))
                .andReturn().getResponse().getContentAsString();
        labId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions reserve(String start, String end, int attendees, String email) throws Exception {
        return mvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content("""
                {"laboratoryId": %d, "requesterName": "Kauan", "requesterEmail": "%s",
                 "purpose": "Aula pratica", "attendees": %d,
                 "startsAt": "%s", "endsAt": "%s"}
                """.formatted(labId, email, attendees, start, end)));
    }

    private ResultActions reserve(String start, String end) throws Exception {
        return reserve(start, end, 10, "aluno@facens.br");
    }

    @Test
    void createsReservation() throws Exception {
        reserve("2026-11-17T14:00", "2026-11-17T16:00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/reservations/")))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.laboratory.code").value("TST-01"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-16T08:00:00"));
    }

    @Test
    void overlappingReservationIsAConflictButBackToBackIsFine() throws Exception {
        reserve("2026-11-17T14:00", "2026-11-17T16:00").andExpect(status().isCreated());

        reserve("2026-11-17T15:00", "2026-11-17T17:00").andExpect(status().isConflict());
        reserve("2026-11-17T13:00", "2026-11-17T14:15").andExpect(status().isConflict());
        reserve("2026-11-17T16:00", "2026-11-17T17:00").andExpect(status().isCreated());
        reserve("2026-11-17T12:00", "2026-11-17T14:00").andExpect(status().isCreated());
    }

    @Test
    void businessRulesReturn422() throws Exception {
        reserve("2026-11-17T17:00", "2026-11-17T19:00")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Laboratory TST-01 is open from 08:00 to 18:00."));
        reserve("2026-11-17T10:00", "2026-11-17T11:00", 21, "aluno@facens.br")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void invalidFieldsReturn400WithDetails() throws Exception {
        reserve("2026-11-17T10:00", "2026-11-17T11:00", 5, "nao-e-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.requesterEmail").exists());
    }

    @Test
    void onlyTheRequesterCanCancelAndCancellingFreesTheSlot() throws Exception {
        String body = reserve("2026-11-18T09:00", "2026-11-18T11:00", 8, "Dono@Facens.br")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(post("/api/reservations/{id}/cancel", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterEmail\": \"outro@facens.br\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/reservations/{id}/cancel", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterEmail\": \"dono@facens.br\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        reserve("2026-11-18T09:00", "2026-11-18T11:00").andExpect(status().isCreated());
    }

    @Test
    void availabilityListsReservedAndFreeSlots() throws Exception {
        reserve("2026-11-19T10:00", "2026-11-19T12:00").andExpect(status().isCreated());
        reserve("2026-11-19T12:00", "2026-11-19T13:00").andExpect(status().isCreated());
        reserve("2026-11-19T17:45", "2026-11-19T18:00").andExpect(status().isUnprocessableContent());

        mvc.perform(get("/api/labs/{id}/availability", labId).param("date", "2026-11-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved", hasSize(2)))
                .andExpect(jsonPath("$.free", hasSize(2)))
                .andExpect(jsonPath("$.free[0].start").value("08:00:00"))
                .andExpect(jsonPath("$.free[0].end").value("10:00:00"))
                .andExpect(jsonPath("$.free[1].start").value("13:00:00"))
                .andExpect(jsonPath("$.free[1].minutes").value(300));
    }

    @Test
    void searchFiltersByEmailAndDate() throws Exception {
        reserve("2026-11-20T08:00", "2026-11-20T09:00", 5, "ana@facens.br").andExpect(status().isCreated());
        reserve("2026-11-20T09:00", "2026-11-20T10:00", 5, "bruno@facens.br").andExpect(status().isCreated());
        reserve("2026-11-23T09:00", "2026-11-23T10:00", 5, "ana@facens.br").andExpect(status().isCreated());

        mvc.perform(get("/api/reservations").param("email", "ANA@facens.br").param("date", "2026-11-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requesterEmail").value("ana@facens.br"));

        mvc.perform(get("/api/reservations").param("laboratoryId", String.valueOf(labId)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].startsAt").value("2026-11-20T08:00:00"));
    }

    @Test
    void labsAreListedAndCodesAreUnique() throws Exception {
        mvc.perform(get("/api/labs")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(6)));

        mvc.perform(post("/api/labs").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "ELE-01", "name": "Duplicado", "building": "Bloco A",
                         "capacity": 10, "opensAt": "08:00", "closesAt": "18:00"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownResourcesReturn404() throws Exception {
        mvc.perform(get("/api/reservations/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/labs/999999/availability").param("date", "2026-11-20"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }
}
