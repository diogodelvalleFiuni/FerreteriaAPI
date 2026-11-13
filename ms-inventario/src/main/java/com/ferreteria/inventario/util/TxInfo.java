package com.ferreteria.inventario.util;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Describe la transaccion que esta activa en el hilo actual. Se usa en los logs de los services para
 * dejar evidencia (informe) de que propagacion se aplico realmente en cada llamada.
 */
public final class TxInfo {

    private TxInfo() {
    }

    public static String estado() {
        return "[txActiva=" + TransactionSynchronizationManager.isActualTransactionActive()
                + ", nombre=" + TransactionSynchronizationManager.getCurrentTransactionName()
                + ", soloLectura=" + TransactionSynchronizationManager.isCurrentTransactionReadOnly() + "]";
    }
}
