// harness-oraculo.js — BANCO EJECUTABLE del ensayo aislado (Kaby, etapa 1)
// Ejecutado el 2026-10-21. Contrato de los constructores A/B + dictamen del Auditor C.
// Regla ÚNICA de tasa: la ÚLTIMA fila CONFIRMADA con hora_fuente <= la hora del
// movimiento; sin ninguna anterior => "sin valuar" (NULL + bandera, JAMÁS 0).
// Montos en CENTAVOS (enteros). Cifras del oráculo: TESTING §1.5 (D-037).
(function () {
  function runHarness() {
    const checks = [];
    const chk = (n, ok) => checks.push({ n, ok });
    const c = x => Math.round(x * 100);
    const f = x => (x / 100).toFixed(2).replace('.', ',');

    const mkRates = () => ([
      { id: 't1', fila: c(880), hora: Date.parse('2026-09-01T08:05:00-04:00'), confirmada: true },
      { id: 't2', fila: c(880), hora: Date.parse('2026-09-20T09:00:00-04:00'), confirmada: true }
    ]);
    let RATES = mkRates();
    function tasaQueVale(h) {
      const prev = RATES.filter(r => r.confirmada && r.hora <= h).sort((a, b) => b.hora - a.hora);
      return prev.length ? prev[0] : null; // null => SIN VALUAR
    }
    function primaDe(q) {
      const r = tasaQueVale(Date.parse(q.fecha));
      if (!r) return { sinValuar: true, prima: null };
      return { sinValuar: false, prima: q.bs - Math.round(q.usdt * r.fila / 100) };
    }

    const m = (id, tipo, nat, monto, fecha, prov) => ({ id, tipo, nat, monto, fecha, prov });
    const v = (id, bs, usdt, fecha, estado, order) => ({ id, bs, usdt, fecha, estado, order });

    const MOV0 = [
      m('m1', 'GASTO', 'NORMAL', c(1000), '2026-09-03T12:30:00-04:00'),
      m('m2', 'GASTO', 'NORMAL', c(350), '2026-09-03T18:10:00-04:00'),
      m('m3', 'INGRESO', 'NORMAL', c(5000), '2026-09-05T10:00:00-04:00', 'papá - remesa'),
      m('m4', 'INGRESO', 'NORMAL', c(200), '2026-09-12T15:00:00-04:00', 'María - pasaje'),
      m('m7', 'GASTO', 'NORMAL', c(120), '2026-09-10T13:00:00-04:00'),
      m('m5', 'GASTO', 'NORMAL', c(120), '2026-09-15T09:30:00-04:00'),
      m('m6', 'INGRESO', 'TURNO_BOLSO', c(400), '2026-09-16T11:00:00-04:00')
    ];
    const V0 = [
      v('v1', c(50000), c(50), '2026-09-07T11:00:00-04:00', 'VIVA', null),
      v('v2', c(24962.5), c(25), '2026-09-14T17:20:00-04:00', 'VIVA', null),
      v('v3', c(10050), c(10), '2026-09-20T10:00:00-04:00', 'VIVA', null)
    ];
    const ANCLA = c(12500);
    const CUADRE_AT = Date.parse('2026-09-21T08:00:00-04:00');

    function computar(ancla, movs, vends) {
      if (ancla === null) return { estado: 'SIN_ANCLA', saldo: null, ingresos: null, gastos: null, ganancia: null };
      let saldo = ancla, ingresos = 0, gastos = 0;
      for (const x of movs) {
        saldo += x.tipo === 'INGRESO' ? x.monto : -x.monto;
        if (x.tipo === 'INGRESO' && x.nat === 'NORMAL') ingresos += x.monto;
        if (x.tipo === 'GASTO' && x.nat === 'NORMAL') gastos += x.monto;
      }
      for (const q of vends) {
        if (q.estado !== 'VIVA') continue; // JUBILADA cuenta 0
        saldo += q.bs; ingresos += q.bs;   // métricas de ingresos (D-055)
      }
      return { estado: 'CALCULADO', saldo, ingresos, gastos, ganancia: ingresos - gastos };
    }
    function estadoTrasEditar(fechaEdicion, ultimoCuadreAt) {
      return Date.parse(fechaEdicion) <= ultimoCuadreAt ? 'DESCUADRE_ABIERTO' : 'CALCULADO';
    }
    function mantenerCuadreAbiertoSinCorreccion(estado, movs) {
      return { estado, movimientos: movs.slice() };
    }
    function upsertPorOrderNumber(rows, incoming) {
      if (incoming.order == null) return rows.concat([incoming]);
      const index = rows.findIndex(row => row.order === incoming.order);
      if (index < 0) return rows.concat([incoming]);
      const updated = rows.slice();
      updated[index] = incoming;
      return updated;
    }

    // ===== EL ORÁCULO =====
    const S = computar(ANCLA, MOV0, V0);
    chk('01 gastados = 1.590,00', S.gastos === c(1590));
    chk('02 ingresos = 90.212,50', S.ingresos === c(90212.5));
    chk('03 saldo @2026-09-21 = 101.522,50', S.saldo === c(101522.5));
    chk('04 ganancia neta = 88.622,50', S.ganancia === c(88622.5));
    const p1 = primaDe(V0[0]), p2 = primaDe(V0[1]), p3 = primaDe(V0[2]);
    chk('05 prima v1 +6.000,00 (con signo)', !p1.sinValuar && p1.prima === c(6000));
    chk('06 prima v2 +2.962,50', !p2.sinValuar && p2.prima === c(2962.5));
    chk('07 prima v3 +1.250,00', !p3.sinValuar && p3.prima === c(1250));
    chk('08 suma primas +10.212,50 y NO toca el saldo', (p1.prima + p2.prima + p3.prima) === c(10212.5) && S.saldo === c(101522.5));
    // ===== MATRIZ DE SIGNO =====
    const soloM6 = computar(0, [MOV0.find(x => x.id === 'm6')], []);
    chk('09 m6 INGRESO/TURNO_BOLSO: saldo +400, metricas 0', soloM6.saldo === c(400) && soloM6.ingresos === 0 && soloM6.gastos === 0);
    const conAjuste = computar(ANCLA, MOV0.concat([m('aj', 'INGRESO', 'AJUSTE_CUADRE', c(1.5), '2026-09-21T09:00:00-04:00')]), V0);
    chk('10 AJUSTE_CUADRE: saldo +1,50; metricas 0', conAjuste.saldo === S.saldo + c(1.5) && conAjuste.ingresos === S.ingresos && conAjuste.gastos === S.gastos);
    // ===== EDICION <= CUADRE =====
    const MOV_EDIT = MOV0.map(x => x.id === 'm7' ? m('m7', 'GASTO', 'NORMAL', c(118.5), x.fecha) : x);
    const pre = computar(ANCLA, MOV_EDIT, V0);
    const estadoEdicion = estadoTrasEditar('2026-09-10T13:00:00-04:00', CUADRE_AT);
    chk('11 pre-edicion 101.524,00; delta -1,50; fecha<=cuadre deja DESCUADRE_ABIERTO', pre.saldo === c(101524) && (S.saldo - pre.saldo) === c(-1.5) && estadoEdicion === 'DESCUADRE_ABIERTO');
    const pendiente = mantenerCuadreAbiertoSinCorreccion(estadoEdicion, MOV_EDIT);
    chk('12 sin correccion, el descuadre sigue abierto y no se inventa un ajuste', pendiente.estado === 'DESCUADRE_ABIERTO' && pendiente.movimientos.length === MOV_EDIT.length && !pendiente.movimientos.some(x => x.nat === 'AJUSTE_CUADRE'));
    // ===== JUBILADA + DEDUP =====
    const VJ = [Object.assign({}, V0[0], { estado: 'JUBILADA' }), v('v1-api', c(50000), c(50), '2026-09-07T11:00:00-04:00', 'VIVA', 'BN-77'), V0[1], V0[2]];
    const SJ = computar(ANCLA, MOV0, VJ);
    chk('13 JUBILADA cuenta 0 + API identica: oraculo intacto', SJ.saldo === S.saldo && SJ.ingresos === S.ingresos && SJ.gastos === S.gastos && SJ.ganancia === S.ganancia);
    let importadas = [];
    const ventaApi = v('v1-api', c(50000), c(50), '2026-09-07T11:00:00-04:00', 'VIVA', 'BN-77');
    importadas = upsertPorOrderNumber(importadas, ventaApi);
    importadas = upsertPorOrderNumber(importadas, ventaApi);
    chk('14 upsert repetido de orderNumber BN-77 conserva una sola fila', importadas.filter(x => x.order === 'BN-77').length === 1);
    // ===== SIN ANCLA =====
    const SN = computar(null, MOV0, V0);
    chk('15 sin ancla: SIN_ANCLA, saldo null (no se inventa)', SN.estado === 'SIN_ANCLA' && SN.saldo === null);
    // ===== SIN TASA =====
    RATES = [];
    const todasSin = V0.map(primaDe);
    const totalSinTasa = computar(ANCLA, MOV0, V0);
    chk('16 sin ninguna tasa: primas NULL, Bs siguen sumando al saldo e ingresos', todasSin.every(p => p.sinValuar === true && p.prima === null) && totalSinTasa.saldo === S.saldo && totalSinTasa.ingresos === c(90212.5) && totalSinTasa.gastos === S.gastos);
    RATES = mkRates();
    const bNeg = primaDe(v('vNeg', c(6450), c(7.5), '2026-09-16T12:00:00-04:00', 'VIVA', null));
    chk('17 prima NEGATIVA: -150,00 con signo', bNeg.prima === c(-150) && bNeg.sinValuar === false);
    const bSin = primaDe(v('vSin', c(6450), c(7.5), '2026-08-30T12:00:00-04:00', 'VIVA', null));
    chk('18 venta antes de la 1a tasa: sin valuar, prima null (JAMAS 0)', bSin.sinValuar === true && bSin.prima === null);

    const passed = checks.filter(k => k.ok).length;
    return {
      passed, total: checks.length,
      failed: checks.filter(k => !k.ok).map(k => k.n),
      oraculo: { gastados: f(S.gastos), ingresos: f(S.ingresos), saldo: f(S.saldo), ganancia: f(S.ganancia) },
      primas: [f(p1.prima), f(p2.prima), f(p3.prima)]
    };
  }

  // Ejecución directa (corregido 2026-10-21): imprime el resultado y falla con código != 0
  // si algún chequeo no pasa. Un archivo que no imprime nada no es evidencia de nada.
  const R = runHarness();
  console.log(JSON.stringify(R, null, 2));
  if (R.passed !== R.total) process.exit(1);
})();
