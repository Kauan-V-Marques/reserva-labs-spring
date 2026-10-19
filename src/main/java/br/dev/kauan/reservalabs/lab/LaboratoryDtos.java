package br.dev.kauan.reservalabs.lab;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public final class LaboratoryDtos {

    private LaboratoryDtos() {
    }

    public record LaboratoryRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}-\\d{2}", message = "must look like ELE-01") String code,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 60) String building,
            @NotNull @Positive @Max(200) Integer capacity,
            @NotNull LocalTime opensAt,
            @NotNull LocalTime closesAt,
            Boolean active) {
    }

    public record LaboratoryResponse(
            Long id, String code, String name, String building, int capacity,
            LocalTime opensAt, LocalTime closesAt, boolean active) {

        public static LaboratoryResponse from(Laboratory lab) {
            return new LaboratoryResponse(lab.getId(), lab.getCode(), lab.getName(), lab.getBuilding(),
                    lab.getCapacity(), lab.getOpensAt(), lab.getClosesAt(), lab.isActive());
        }
    }
}
