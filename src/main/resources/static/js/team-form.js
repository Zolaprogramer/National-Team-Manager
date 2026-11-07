// Prikazuje zastavu dok korisnik upisuje državu ili bira sliku. Konačnu zastavu određuje server pri čuvanju.
document.addEventListener("DOMContentLoaded", () => {
    const countryInput = document.getElementById("countryName");
    const fileInput = document.getElementById("flag");
    const choiceSelect = document.getElementById("flagChoice");
    const preview = document.getElementById("flagPreview");
    const hint = document.getElementById("flagHint");
    const { previewUrl, defaultUrl, currentUrl, customFlag, recognizedText, unrecognizedText, uploadText, noneText } = preview.dataset;
    let timer = null;

    async function showAutomaticFlag() {
        const response = await fetch(`${previewUrl}?country=${encodeURIComponent(countryInput.value)}`);
        const flag = await response.json();
        preview.src = flag.url;
        hint.textContent = flag.recognized ? recognizedText : unrecognizedText;
    }

    function refresh() {
        const file = fileInput.files[0];
        if (file) {
            preview.src = URL.createObjectURL(file);
            hint.textContent = uploadText;
        } else if (choiceSelect.value === "NONE") {
            preview.src = defaultUrl;
            hint.textContent = noneText;
        } else if (choiceSelect.value === "KEEP" && customFlag === "true") {
            preview.src = currentUrl;
            hint.textContent = uploadText;
        } else {
            showAutomaticFlag();
        }
    }

    countryInput.addEventListener("input", () => {
        clearTimeout(timer);
        timer = setTimeout(refresh, 250);
    });
    fileInput.addEventListener("change", refresh);
    choiceSelect.addEventListener("change", refresh);
    refresh();
});
