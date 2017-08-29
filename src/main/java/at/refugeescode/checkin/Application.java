package at.refugeescode.checkin;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@EntityScan(basePackageClasses = {Application.class, Jsr310JpaConverters.class})
@SpringBootApplication
@Slf4j
public class Application {

    /**
     * When this marker is used, an email will be sent to the administrator.
     */
    public static final Marker SEND_MAIL = MarkerFactory.getMarker("SEND_MAIL");

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        log.info(Application.SEND_MAIL, "Application startup");
    }
}
