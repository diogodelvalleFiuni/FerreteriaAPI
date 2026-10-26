package com.ferreteria.ventas.mapper;

import com.ferreteria.entity.general.Estado;
import com.ferreteria.entity.general.Producto;
import com.ferreteria.entity.ventas.Cliente;
import com.ferreteria.entity.ventas.FacturaVenta;
import com.ferreteria.entity.ventas.Presupuesto;

/**
 * Convierte un id (el que viaja en los DTO) en una referencia de entidad que lleva solo el id,
 * sin consultar la BD. Los mappers que la extienden la usan automaticamente (MapStruct resuelve
 * Long -> entidad por tipo). Si el id no existe, la FK lo rechaza y el GlobalExceptionHandler
 * responde 409.
 */
public interface EntityReferenceMapper {

    default Cliente clienteFromId(Long id) {
        if (id == null) {
            return null;
        }
        Cliente cliente = new Cliente();
        cliente.setIdPersona(id);
        return cliente;
    }

    default Estado estadoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Estado estado = new Estado();
        estado.setIdEstado(id);
        return estado;
    }

    default Producto productoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Producto producto = new Producto();
        producto.setIdProducto(id);
        return producto;
    }

    default Presupuesto presupuestoFromId(Long id) {
        if (id == null) {
            return null;
        }
        Presupuesto presupuesto = new Presupuesto();
        presupuesto.setIdPresupuesto(id);
        return presupuesto;
    }

    default FacturaVenta facturaFromId(Long id) {
        if (id == null) {
            return null;
        }
        FacturaVenta factura = new FacturaVenta();
        factura.setIdFactura(id);
        return factura;
    }
}
