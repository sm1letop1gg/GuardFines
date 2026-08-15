package ru.guard.fines;

public enum FineStatus {
    UNPAID("Не выплачен"),
    PAID("Выполнен"),
    OVERDUE("Просрочен"),
    CANCELLED("Отменён");

    private final String displayName;

    FineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isOpen() {
        return this == UNPAID || this == OVERDUE;
    }
}
