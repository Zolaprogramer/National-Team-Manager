// Taktička tabla: igrači se raspoređuju prevlačenjem ili klikom na igrača pa na odredište.
// Stanje se drži samo u pregledniku dok se ne klikne "Sačuvaj taktiku".
// Sva pravila se ponovo provjeravaju na serveru u klasama LineupService i TacticsService.
document.addEventListener("DOMContentLoaded", () => {
    const config = window.TACTICS_BOARD;
    const text = config.text;
    const players = new Map(config.players.map((player) => [player.id, player]));

    const form = document.getElementById("tacticsForm");
    const formationSelect = document.getElementById("formation");
    const boardPlayers = document.getElementById("boardPlayers");
    const benchList = document.getElementById("benchList");
    const outList = document.getElementById("outList");
    const dutyList = document.getElementById("dutyList");
    const message = document.getElementById("boardMessage");
    const hiddenFields = document.getElementById("hiddenFields");
    const roleSelects = [...document.querySelectorAll(".role-select")];

    const initial = {
        formation: formationSelect.value,
        slots: config.starterIds.map((id) => (players.has(id) ? id : null)),
        bench: config.substituteIds.filter((id) => players.has(id)),
        duties: {...config.duties},
        roles: roleSelects.map((select) => select.value)
    };
    let state = cloneState(initial);
    let selectedId = null;

    function cloneState(source) {
        return {
            slots: [...source.slots],
            bench: [...source.bench],
            duties: {...source.duties}
        };
    }

    function locationOf(id) {
        const slot = state.slots.indexOf(id);
        if (slot >= 0) {
            return {zone: "pitch", slot};
        }
        return {zone: state.bench.includes(id) ? "bench" : "out"};
    }

    function outPlayers() {
        return config.players.filter((player) => locationOf(player.id).zone === "out").map((player) => player.id);
    }

    function currentSlots() {
        return config.formationSlots[formationSelect.value];
    }

    function canPlayInSlot(id, slot) {
        return (slot === 0) === players.get(id).goalkeeper;
    }

    function showMessage(textValue, kind = "info") {
        message.textContent = textValue;
        message.className = `alert alert-${kind} py-2 small`;
    }

    function format(template, ...values) {
        return values.reduce((result, value, index) => result.replace(`{${index}}`, value), template);
    }

    /** Lista "van sastava" se izvodi iz terena i klupe, pa se u njoj ništa ne upisuje. */
    function replaceAt(location, oldId, newId) {
        if (location.zone === "pitch") {
            state.slots[location.slot] = newId;
        } else if (location.zone === "bench") {
            state.bench[state.bench.indexOf(oldId)] = newId;
        }
    }

    /** Zamjenjuje mjesta dvojici igrača. Dva igrača iz iste liste nemaju šta da zamijene. */
    function exchangePlayers(firstId, secondId) {
        const first = locationOf(firstId);
        const second = locationOf(secondId);
        if (first.zone === second.zone && first.zone !== "pitch") {
            return false;
        }
        if ((first.zone === "pitch" && !canPlayInSlot(secondId, first.slot))
            || (second.zone === "pitch" && !canPlayInSlot(firstId, second.slot))) {
            showMessage(text.goalkeeperRule, "danger");
            return false;
        }
        replaceAt(first, firstId, secondId);
        replaceAt(second, secondId, firstId);
        showMessage(format(text.swapped, players.get(firstId).lastName, players.get(secondId).lastName), "success");
        return true;
    }

    function placeInEmptySlot(id, slot) {
        if (!canPlayInSlot(id, slot)) {
            showMessage(text.goalkeeperRule, "danger");
            return false;
        }
        const location = locationOf(id);
        if (location.zone === "pitch") {
            state.slots[location.slot] = null;
        } else if (location.zone === "bench") {
            state.bench = state.bench.filter((benchId) => benchId !== id);
        }
        state.slots[slot] = id;
        return true;
    }

    function moveToZone(id, zone) {
        const location = locationOf(id);
        if (location.zone === zone) {
            return false;
        }
        if (location.zone === "pitch") {
            showMessage(text.starterMustStay, "warning");
            return false;
        }
        if (zone === "bench") {
            if (state.bench.length >= config.maxSubstitutes) {
                showMessage(text.benchFull, "warning");
                return false;
            }
            state.bench.push(id);
        } else {
            state.bench = state.bench.filter((benchId) => benchId !== id);
        }
        return true;
    }

    function syncRoles() {
        const starters = state.slots.filter(Boolean).map(String);
        let cleared = false;
        roleSelects.forEach((select) => {
            [...select.options].forEach((option) => {
                option.disabled = Boolean(option.value) && !starters.includes(option.value);
            });
            if (select.value && !starters.includes(select.value)) {
                select.value = "";
                cleared = true;
            }
        });
        if (cleared) {
            showMessage(text.rolesCleared, "warning");
        }
    }

    function roleBadgesOf(id) {
        const badges = {captain: text.badgeCaptain, penalty: text.badgePenalty, freeKick: text.badgeFreeKick, corner: text.badgeCorner};
        return roleSelects.filter((select) => select.value === String(id)).map((select) => badges[select.dataset.role]);
    }

    function photoOf(player) {
        const image = document.createElement("img");
        image.className = "pitch-photo";
        image.alt = player.fullName;
        image.draggable = false;
        image.src = player.photoUrl || config.silhouetteUrl;
        image.addEventListener("error", () => {
            image.src = config.silhouetteUrl;
        }, {once: true});
        return image;
    }

    function makeInteractive(element, id) {
        element.tabIndex = 0;
        element.setAttribute("role", "button");
        element.setAttribute("aria-pressed", String(id === selectedId));
        element.draggable = true;
        element.dataset.playerId = id;
        element.classList.toggle("is-selected", id === selectedId);
        element.addEventListener("click", (event) => {
            event.stopPropagation();
            clickPlayer(id);
        });
        element.addEventListener("keydown", (event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                clickPlayer(id);
            }
        });
        element.addEventListener("dragstart", (event) => {
            event.dataTransfer.setData("text/plain", String(id));
            event.dataTransfer.effectAllowed = "move";
        });
        allowDrop(element, (draggedId) => {
            if (draggedId !== id && exchangePlayers(draggedId, id)) {
                selectedId = null;
            }
        });
    }

    function allowDrop(element, onDrop) {
        element.addEventListener("dragover", (event) => {
            event.preventDefault();
            element.classList.add("is-drop-target");
        });
        element.addEventListener("dragleave", () => element.classList.remove("is-drop-target"));
        element.addEventListener("drop", (event) => {
            event.preventDefault();
            event.stopPropagation();
            element.classList.remove("is-drop-target");
            onDrop(Number(event.dataTransfer.getData("text/plain")));
            render();
        });
    }

    function clickPlayer(id) {
        if (selectedId === null) {
            selectedId = id;
            showMessage(format(text.selected, players.get(id).fullName));
        } else if (selectedId === id) {
            selectedId = null;
            showMessage(text.hint);
        } else if (exchangePlayers(selectedId, id)) {
            selectedId = null;
        } else if (locationOf(selectedId).zone === locationOf(id).zone && locationOf(id).zone !== "pitch") {
            selectedId = id;
            showMessage(format(text.selected, players.get(id).fullName));
        }
        render();
    }

    function renderPitch() {
        const slots = currentSlots();
        boardPlayers.replaceChildren(...slots.map((slot) => {
            const id = state.slots[slot.index];
            const marker = document.createElement("div");
            marker.className = "pitch-player board-player";
            marker.style.left = `${slot.x}%`;
            marker.style.top = `${slot.y}%`;
            marker.dataset.slot = slot.index;

            if (id === null) {
                marker.classList.add("empty");
                const shirt = document.createElement("div");
                shirt.className = "pitch-shirt";
                shirt.textContent = slot.label;
                marker.append(shirt);
                marker.title = text.emptySlot;
                marker.addEventListener("click", () => {
                    if (selectedId !== null && placeInEmptySlot(selectedId, slot.index)) {
                        selectedId = null;
                    }
                    render();
                });
                allowDrop(marker, (draggedId) => placeInEmptySlot(draggedId, slot.index));
                return marker;
            }

            const player = players.get(id);
            marker.classList.toggle("goalkeeper", player.goalkeeper);
            marker.classList.toggle("misplaced", !player.goalkeeper && player.position !== slot.label);
            marker.title = player.fullName;

            const shirt = document.createElement("div");
            shirt.className = "pitch-shirt";
            shirt.append(photoOf(player));
            const badges = roleBadgesOf(id);
            if (badges.length) {
                const badge = document.createElement("span");
                badge.className = "pitch-captain";
                badge.textContent = badges.join("");
                shirt.append(badge);
            }
            marker.append(shirt);

            const name = document.createElement("div");
            name.className = "pitch-name";
            const number = document.createElement("strong");
            number.textContent = player.number;
            name.append(number, ` ${player.lastName}`);
            marker.append(name);

            const position = document.createElement("div");
            position.className = "pitch-position";
            position.textContent = player.position;
            marker.append(position);

            makeInteractive(marker, id);
            return marker;
        }));
    }

    function renderList(container, ids) {
        if (!ids.length) {
            const empty = document.createElement("div");
            empty.className = "small text-secondary p-2";
            empty.textContent = text.emptyList;
            container.replaceChildren(empty);
            return;
        }
        container.replaceChildren(...ids.map((id) => {
            const player = players.get(id);
            const row = document.createElement("div");
            row.className = "bench-player";

            const photo = photoOf(player);
            photo.className = "avatar";
            const number = document.createElement("span");
            number.className = "jersey";
            number.textContent = player.number;
            const name = document.createElement("span");
            name.className = "bench-name";
            name.textContent = player.fullName;
            const position = document.createElement("span");
            position.className = "small text-secondary";
            position.textContent = player.position;

            row.append(photo, number, name, position);
            makeInteractive(row, id);
            return row;
        }));
    }

    function renderDuties() {
        dutyList.replaceChildren(...state.slots.filter(Boolean).map((id) => {
            const player = players.get(id);
            const row = document.createElement("div");
            row.className = "duty-row";

            const label = document.createElement("label");
            label.htmlFor = `duty${id}`;
            label.className = "duty-name";
            label.textContent = `${player.number} ${player.lastName}`;

            const select = document.createElement("select");
            select.id = `duty${id}`;
            select.className = "form-select form-select-sm";
            config.dutyOptions.forEach((option) => select.add(new Option(option.label, option.value)));
            select.value = state.duties[id] || "BALANCED";
            select.addEventListener("change", () => {
                state.duties[id] = select.value;
                writeHiddenFields();
            });

            row.append(label, select);
            return row;
        }));
    }

    function hiddenField(name, value) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value;
        return input;
    }

    function writeHiddenFields() {
        const fields = [];
        state.slots.forEach((id, slot) => fields.push(hiddenField(`starterIds[${slot}]`, id === null ? "" : id)));
        state.bench.forEach((id) => fields.push(hiddenField("substituteIds", id)));
        [...state.slots.filter(Boolean), ...state.bench].forEach((id) => {
            fields.push(hiddenField(`duties[${id}]`, state.duties[id] || "BALANCED"));
        });
        hiddenFields.replaceChildren(...fields);
    }

    function render() {
        syncRoles();
        renderPitch();
        renderList(benchList, state.bench);
        renderList(outList, outPlayers());
        renderDuties();
        writeHiddenFields();
        document.getElementById("benchCount").textContent = state.bench.length;
        document.getElementById("outCount").textContent = outPlayers().length;
        if (selectedId !== null) {
            document.querySelector(`[data-player-id="${selectedId}"]`)?.focus({preventScroll: true});
        }
    }

    document.querySelectorAll("[data-zone]").forEach((zone) => {
        zone.addEventListener("click", () => {
            if (selectedId !== null && moveToZone(selectedId, zone.dataset.zone)) {
                selectedId = null;
                render();
            }
        });
        allowDrop(zone, (draggedId) => moveToZone(draggedId, zone.dataset.zone));
    });

    formationSelect.addEventListener("change", () => {
        selectedId = null;
        showMessage(text.formationChanged);
        render();
    });

    roleSelects.forEach((select) => select.addEventListener("change", render));

    document.getElementById("resetBoard").addEventListener("click", () => {
        state = cloneState(initial);
        formationSelect.value = initial.formation;
        roleSelects.forEach((select, index) => {
            select.value = initial.roles[index];
        });
        selectedId = null;
        showMessage(text.reset);
        render();
    });

    form.addEventListener("submit", (event) => {
        if (state.slots.filter(Boolean).length !== 11) {
            event.preventDefault();
            showMessage(text.needEleven, "danger");
        } else if (!document.getElementById("captainId").value) {
            event.preventDefault();
            showMessage(text.needCaptain, "danger");
        } else {
            writeHiddenFields();
        }
    });

    render();
});
