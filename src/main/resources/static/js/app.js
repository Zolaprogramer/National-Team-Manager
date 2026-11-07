// Potvrda prije brisanja: svaki obrazac s atributom data-confirm prvo otvara dijalog.
document.addEventListener("DOMContentLoaded", () => {
    const modalElement = document.getElementById("confirmModal");
    if (!modalElement) {
        return;
    }
    const modal = new bootstrap.Modal(modalElement);
    const message = document.getElementById("confirmModalMessage");
    const confirmButton = document.getElementById("confirmModalButton");
    let pendingForm = null;

    document.querySelectorAll("form[data-confirm]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            event.preventDefault();
            pendingForm = form;
            message.textContent = form.dataset.confirm;
            modal.show();
        });
    });

    confirmButton.addEventListener("click", () => {
        if (pendingForm) {
            pendingForm.submit();
        }
    });
});
