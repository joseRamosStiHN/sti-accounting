CREATE MATERIALIZED VIEW libro_diario AS
SELECT
  t.fecha,
  t.descripcion_pda AS descripcion,
  t.referencia,
  t.tipo_documento,
  t.tasa_cambio,
  t.moneda,
  c.codigo AS codigo_cuenta,
  c.descripcion AS nombre_cuenta,
  padre.codigo AS codigo_cuenta_padre,
  padre.descripcion AS nombre_cuenta_padre,
  dt.monto,
  dt.movimiento,
  CASE 
    WHEN dt.movimiento = 'D' THEN dt.monto 
    ELSE NULL 
  END AS monto_debito,
  CASE 
    WHEN dt.movimiento = 'C' THEN dt.monto 
    ELSE NULL 
  END AS monto_credito
FROM
  transacciones t
JOIN
  detalle_transaccion dt ON t.id = dt.id_transaccion
JOIN
  cuentas c ON dt.id_cuenta = c.id
LEFT JOIN
  cuentas padre ON c.parent_id = padre.id;