package br.dev.kauan.reservalabs.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável: o "agora" usa o fuso da faculdade e pode ser fixado nos testes. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
