package com.restaurant.reservation.dto;

import com.restaurant.reservation.model.DiningTable;
import com.restaurant.reservation.model.SeatingArea;
import com.restaurant.reservation.model.TableStatus;

public record TableResponse(int tableNumber, int capacity, SeatingArea area, TableStatus status) {

    public static TableResponse from(DiningTable table) {
        return new TableResponse(
                table.getTableNumber(), table.getCapacity(), table.getArea(), table.getStatus());
    }
}
