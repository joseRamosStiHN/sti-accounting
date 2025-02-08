package com.sti.accounting.utils;

/*
* TODO: esta clase provoca un thread starvation se reemplazo por un servicio
*  se debe tener cuidado cuando usa threads, debe poder gestionar la liberación de los recursos
* */
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}