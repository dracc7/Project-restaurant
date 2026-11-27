"use strict";

const tableList = document.querySelector("#table-list");
const tableTemplate = document.querySelector("#table-card");
const tableFilter = document.querySelector("#table-filter");
const tableMessage = document.querySelector("#table-message");
const reservationMessage = document.querySelector("#reservation-message");
const reservationDetails = document.querySelector("#reservation-details");
const assignmentForm = document.querySelector("#assignment-form");
const availableTable = document.querySelector("#available-table");
const assignButton = document.querySelector("#assign-button");
const seatButton = document.querySelector("#seat-button");
const completeButton = document.querySelector("#complete-button");
const noShowButton = document.querySelector("#no-show-button");

let tables = [];
let reservation = null;
let busy = false;

function formatLabel(value) {
    const label = value.toLowerCase().replaceAll("_", " ");
    return label[0].toUpperCase() + label.slice(1);
}

function showMessage(element, message = "", isError = false) {
    element.textContent = message;
    element.dataset.error = String(isError);
}

async function request(path, method = "GET", body) {
    const options = { method };
    if (body !== undefined) {
        options.headers = { "Content-Type": "application/json" };
        options.body = JSON.stringify(body);
    }
    const response = await fetch(`/api/${path}`, options);
    if (!response.ok) {
        const messages = {
            400: "Check the information entered and try again.",
            404: "The table or reservation could not be found.",
            409: "This action conflicts with the current table or reservation status. Refresh and check again."
        };
        throw new Error(messages[response.status] ?? "The server could not complete the request. Try again.");
    }
    return response.json();
}

function setControls() {
    for (const control of document.querySelectorAll("button, select")) {
        control.disabled = busy;
    }
    const hasOptions = availableTable.options.length > 0 && availableTable.value !== "";
    availableTable.disabled = busy || !hasOptions;
    assignButton.disabled = busy || !hasOptions;
}

async function runAction(action, messageElement) {
    if (busy) return;
    busy = true;
    setControls();
    showMessage(messageElement);
    try {
        await action();
    } catch (error) {
        showMessage(messageElement, error.message, true);
    } finally {
        busy = false;
        setControls();
    }
}

function showBadge(badge, status) {
    badge.textContent = formatLabel(status);
    badge.dataset.status = status;
}

function renderTables() {
    const visible = tables.filter(table => !tableFilter.value || table.status === tableFilter.value);
    const ready = tables.filter(table => table.status === "READY").length;
    document.querySelector("#table-summary").textContent = `${tables.length} tables · ${ready} ready`;
    const cards = visible.map(table => {
        const card = tableTemplate.content.cloneNode(true);
        card.querySelector("h3").textContent = `Table ${table.tableNumber}`;
        showBadge(card.querySelector(".badge"), table.status);
        card.querySelector("p").textContent = `${formatLabel(table.area)} · ${table.capacity} seats`;
        card.querySelector(".sr-only").textContent = `Change status for table ${table.tableNumber}`;
        const select = card.querySelector("select");
        select.value = table.status;
        select.addEventListener("change", async () => {
            const status = select.value;
            await runAction(async () => {
                const updated = await request(`tables/${table.tableNumber}/status`, "PUT", { status });
                tables = tables.map(current => current.tableNumber === updated.tableNumber ? updated : current);
                showMessage(tableMessage, `Table ${updated.tableNumber} is now ${formatLabel(updated.status).toLowerCase()}.`);
                renderTables();
                await refreshAvailability();
            }, tableMessage);
            select.value = table.status;
        });
        return card;
    });
    tableList.replaceChildren(...cards);
    if (!visible.length && !tableMessage.textContent) {
        showMessage(tableMessage, "No tables match this view.");
    }
    setControls();
}

async function refreshTables() {
    tables = await request("tables");
    showMessage(tableMessage);
    renderTables();
}

function renderReservation() {
    reservationDetails.hidden = reservation === null;
    if (!reservation) return;

    document.querySelector("#customer-name").textContent = reservation.customerName;
    showBadge(document.querySelector("#reservation-status"), reservation.status);
    document.querySelector("#reservation-time").textContent = new Date(reservation.startTime)
        .toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
    document.querySelector("#party-size").textContent = reservation.partySize;
    document.querySelector("#seating-preference").textContent = formatLabel(reservation.seatingPreference);
    const hasTable = reservation.tableNumber !== null;
    document.querySelector("#assigned-table").textContent = hasTable ? `Table ${reservation.tableNumber}` : "Unassigned";

    const isBooked = reservation.status === "BOOKED";
    assignmentForm.hidden = !isBooked;
    seatButton.hidden = !isBooked || !hasTable;
    completeButton.hidden = reservation.status !== "SEATED";
    noShowButton.hidden = !isBooked;
    availableTable.replaceChildren();
}

async function refreshAvailability() {
    if (reservation?.status !== "BOOKED") return;
    availableTable.replaceChildren(new Option("Checking availability…", ""));
    try {
        const code = encodeURIComponent(reservation.confirmationCode);
        const available = await request(`tables/available?confirmationCode=${code}`);
        const options = available.map(table =>
            new Option(`Table ${table.tableNumber} · ${formatLabel(table.area)} · ${table.capacity} seats`, table.tableNumber));
        availableTable.replaceChildren(...(options.length ? options : [new Option("No matching tables", "")]));
        const assigned = String(reservation.tableNumber);
        if (options.some(option => option.value === assigned)) {
            availableTable.value = assigned;
        }
    } catch (error) {
        availableTable.replaceChildren(new Option("Availability unavailable", ""));
        showMessage(reservationMessage, error.message, true);
    }
}

function runReservationAction(path, successMessage) {
    return runAction(async () => {
        reservation = await request(path, "PUT");
        renderReservation();
        showMessage(reservationMessage, successMessage);
        await Promise.all([refreshTables(), refreshAvailability()]);
    }, reservationMessage);
}

function reservationPath(tableNumber) {
    const code = encodeURIComponent(reservation.confirmationCode);
    return `tables/${tableNumber}/reservations/${code}`;
}

document.querySelector("#lookup-form").addEventListener("submit", event => {
    event.preventDefault();
    const input = document.querySelector("#confirmation-code");
    const code = input.value.trim();
    if (!code) {
        showMessage(reservationMessage, "Enter a confirmation code.", true);
        return;
    }
    runAction(async () => {
        reservation = null;
        renderReservation();
        reservation = await request(`reservations/${encodeURIComponent(code)}`);
        renderReservation();
        await refreshAvailability();
    }, reservationMessage);
});

assignmentForm.addEventListener("submit", event => {
    event.preventDefault();
    if (reservation && availableTable.value) {
        runReservationAction(reservationPath(availableTable.value), "Table assigned.");
    }
});

seatButton.addEventListener("click", () =>
    runReservationAction(`${reservationPath(reservation.tableNumber)}/seated`, "Party seated."));
completeButton.addEventListener("click", () =>
    runReservationAction(`${reservationPath(reservation.tableNumber)}/completed`, "Visit completed."));
noShowButton.addEventListener("click", () => {
    if (window.confirm("Mark this reservation as a no-show?")) {
        const code = encodeURIComponent(reservation.confirmationCode);
        runReservationAction(`tables/reservations/${code}/no-show`, "Reservation marked no-show.");
    }
});

tableFilter.addEventListener("change", () => {
    showMessage(tableMessage);
    renderTables();
});

document.querySelector("#refresh-tables").addEventListener("click", () =>
    runAction(() => Promise.all([refreshTables(), refreshAvailability()]), tableMessage));

runAction(refreshTables, tableMessage);
