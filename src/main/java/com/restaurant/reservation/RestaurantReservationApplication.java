package com.restaurant.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The starting point of the whole app.
 *
 * In Eclipse: right-click this file → Run As → Java Application.
 * The server starts on http://localhost:8080
 *
 * @SpringBootApplication tells Spring to scan this package and every
 * package under it (model, repository, service, controller, ...) and
 * wire everything together automatically. That's why every class must
 * live somewhere under com.restaurant.reservation.
 */
@SpringBootApplication
public class RestaurantReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantReservationApplication.class, args);
    }
}
