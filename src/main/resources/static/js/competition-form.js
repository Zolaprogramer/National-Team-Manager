// Prikazuje izabranu sliku prije čuvanja; tip i veličinu fajla ponovo provjerava server.
document.addEventListener("DOMContentLoaded", () => {
    const input = document.getElementById("logo");
    const preview = document.getElementById("logoPreview");
    const removeLogo = document.getElementById("removeLogo1");
    if (!input || !preview) {
        return;
    }
    const savedUrl = preview.src;

    function refresh() {
        const file = input.files[0];
        if (file) {
            preview.src = URL.createObjectURL(file);
        } else if (removeLogo && removeLogo.checked) {
            preview.src = preview.dataset.defaultUrl;
        } else {
            preview.src = savedUrl;
        }
    }

    input.addEventListener("change", refresh);
    if (removeLogo) {
        removeLogo.addEventListener("change", refresh);
    }
});
