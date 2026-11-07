// Uživo prikazuje izabrane igrače na terenu i sprečava da isti igrač bude izabran dva puta.
// Sva pravila se ponovo provjeravaju na serveru u klasi LineupService.
document.addEventListener("DOMContentLoaded", () => {
    const formationSelect = document.getElementById("formation");
    const starterSelects = [...document.querySelectorAll("select[data-slot]")];
    const captainSelect = document.getElementById("captainId");
    const substituteBoxes = [...document.querySelectorAll("input[data-substitute]")];
    const pitchPlayers = document.getElementById("pitchPlayers");
    const substituteCounter = document.getElementById("substituteCount");
    const maxSubstitutes = Number(substituteCounter.dataset.max);

    function selectedStarterIds() {
        return starterSelects.map((select) => select.value).filter(Boolean);
    }

    function updateSlotLabels(slots) {
        slots.forEach((slot) => {
            document.querySelector(`[data-slot-label="${slot.index}"]`).textContent = slot.label;
        });
    }

    function renderPitch(slots) {
        pitchPlayers.replaceChildren(...slots.map((slot) => {
            const option = starterSelects[slot.index].selectedOptions[0];
            const filled = Boolean(option && option.value);

            const marker = document.createElement("div");
            marker.className = "pitch-player";
            marker.classList.toggle("goalkeeper", slot.index === 0);
            marker.classList.toggle("empty", !filled);
            marker.style.left = `${slot.x}%`;
            marker.style.top = `${slot.y}%`;

            const shirt = document.createElement("div");
            shirt.className = "pitch-shirt";
            shirt.textContent = filled ? option.dataset.number : slot.label;
            marker.append(shirt);

            if (filled) {
                if (option.value === captainSelect.value) {
                    const captain = document.createElement("span");
                    captain.className = "pitch-captain";
                    captain.textContent = "C";
                    shirt.append(captain);
                }
                const name = document.createElement("div");
                name.className = "pitch-name";
                name.textContent = option.dataset.lastName;
                marker.append(name);
            }
            return marker;
        }));
    }

    function syncChoices() {
        const starters = selectedStarterIds();

        starterSelects.forEach((select) => {
            [...select.options].forEach((option) => {
                option.disabled = Boolean(option.value) && option.value !== select.value && starters.includes(option.value);
            });
        });

        [...captainSelect.options].forEach((option) => {
            option.disabled = Boolean(option.value) && !starters.includes(option.value);
        });
        if (captainSelect.value && !starters.includes(captainSelect.value)) {
            captainSelect.value = "";
        }

        substituteBoxes.forEach((box) => {
            if (starters.includes(box.value)) {
                box.checked = false;
            }
        });
        const checkedCount = substituteBoxes.filter((box) => box.checked).length;
        substituteBoxes.forEach((box) => {
            box.disabled = starters.includes(box.value) || (!box.checked && checkedCount >= maxSubstitutes);
        });
        substituteCounter.textContent = checkedCount;
    }

    function refresh() {
        const slots = window.FORMATION_SLOTS[formationSelect.value];
        syncChoices();
        updateSlotLabels(slots);
        renderPitch(slots);
    }

    [formationSelect, captainSelect, ...starterSelects, ...substituteBoxes].forEach((element) => {
        element.addEventListener("change", refresh);
    });
    refresh();
});
