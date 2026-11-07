// Pri izboru takmičenja odmah prikazuje njegov logo, bez ponovnog učitavanja stranice.
document.addEventListener("DOMContentLoaded", () => {
    const competition = document.getElementById("competitionId");
    const preview = document.getElementById("competitionLogoPreview");
    if (!competition || !preview) {
        return;
    }

    function showLogo() {
        const url = competition.selectedOptions[0]?.dataset.logoUrl;
        preview.hidden = !url;
        if (url) {
            preview.src = url;
            preview.alt = competition.selectedOptions[0].text;
        }
    }

    competition.addEventListener("change", showLogo);
    showLogo();
});

// U izboru stadiona ostavlja samo stadione domaće reprezentacije; to ponovo provjerava MatchService.
document.addEventListener("DOMContentLoaded", () => {
    const homeTeam = document.getElementById("homeTeamId");
    const venue = document.getElementById("stadiumId");
    const customStadium = document.getElementById("customStadium");
    const customInput = document.getElementById("stadium");
    if (!homeTeam || !venue) {
        return;
    }

    function refreshOptions() {
        [...venue.options].forEach((option) => {
            const belongs = !option.value || option.dataset.teamId === homeTeam.value;
            option.hidden = !belongs;
            option.disabled = !belongs;
        });
        if (venue.selectedOptions[0]?.disabled) {
            venue.value = "";
        }
    }

    function refreshCustomInput() {
        customStadium.hidden = Boolean(venue.value);
    }

    /** Pri izboru domaćina predlaže njegov glavni stadion, a korisnik može izabrati i drugi. */
    function selectMainStadium() {
        const main = [...venue.options].find((option) => option.dataset.teamId === homeTeam.value && option.dataset.main === "true");
        if (main && !venue.value && !customInput.value) {
            venue.value = main.value;
        }
    }

    homeTeam.addEventListener("change", () => {
        refreshOptions();
        selectMainStadium();
        refreshCustomInput();
    });
    venue.addEventListener("change", refreshCustomInput);
    refreshOptions();
    refreshCustomInput();
});
