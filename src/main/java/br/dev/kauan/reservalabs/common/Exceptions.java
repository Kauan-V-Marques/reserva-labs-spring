package br.dev.kauan.reservalabs.common;

/** Exceções de negócio traduzidas para Problem Details em {@link ApiExceptionHandler}. */
public final class Exceptions {

    private Exceptions() {
    }

    /** Recurso inexistente (404). */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** Conflito com o estado atual, como horário já reservado (409). */
    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    /** Pedido bem formado, mas que viola uma regra de negócio (422). */
    public static class BusinessRuleException extends RuntimeException {
        public BusinessRuleException(String message) {
            super(message);
        }
    }

    /** Usuário sem permissão para a operação (403). */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
