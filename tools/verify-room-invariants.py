#!/usr/bin/env python3
"""Exercise Room-exported DDL and the app's SQLite invariant triggers with host SQLite."""

from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "app/schemas/com.mofoxx.kaby.data.KabyDatabase/1.json"
DATABASE_SOURCE = ROOT / "app/src/main/java/com/mofoxx/kaby/data/KabyDatabase.kt"


def fail(message: str) -> None:
    raise SystemExit(f"FAIL {message}")


def load_schema_database() -> tuple[sqlite3.Connection, dict[str, dict]]:
    if not SCHEMA.is_file() or not DATABASE_SOURCE.is_file():
        fail("Run Room KSP first so the schema export exists.")
    document = json.loads(SCHEMA.read_text(encoding="utf-8"))
    entities = {entity["tableName"]: entity for entity in document["database"]["entities"]}
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys = ON")
    for table, entity in entities.items():
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table))

    source = DATABASE_SOURCE.read_text(encoding="utf-8")
    trigger_sql = re.findall(
        r'db\.execSQL\(\s*"""(.*?)"""\s*\.trimIndent\(\)\s*,?\s*\)',
        source,
        flags=re.DOTALL,
    )
    if not trigger_sql:
        fail("No invariant trigger SQL found in KabyDatabase.Callback.")
    for sql in trigger_sql:
        connection.executescript(sql)
    return connection, entities


def insert_movement(connection: sqlite3.Connection, values: dict[str, str | None]) -> None:
    columns = ", ".join(f'"{column}"' for column in values)
    placeholders = ", ".join("?" for _ in values)
    connection.execute(
        f"INSERT INTO movimientos ({columns}) VALUES ({placeholders})",
        tuple(values.values()),
    )


def expect_rejected(name: str, operation) -> None:
    try:
        operation()
    except sqlite3.IntegrityError:
        print(f"PASS {name}")
    else:
        fail(f"{name}: invalid row was accepted")


def main() -> None:
    connection, entities = load_schema_database()
    if len(entities) != 14:
        fail(f"Expected 14 Room tables, found {len(entities)}")
    print(f"PASS Room DDL creates {len(entities)} tables")

    base = {
        "tipo": "GASTO",
        "naturaleza": "NORMAL",
        "moneda": "Bs",
        "monto": "12.50",
        "estado_valuacion": "VALUADA",
        "fecha_hora": "2026-09-03T12:30:00-04:00",
        "categoria": "fixture",
        "origen": "MANO",
        "timestamp_creacion": "2026-09-03T12:30:00-04:00",
        "timestamp_ultima_edicion": "2026-09-03T12:30:00-04:00",
    }
    expect_rejected("negative movement amount", lambda: insert_movement(connection, {**base, "monto": "-1"}))
    expect_rejected(
        "third-party income requires provenance",
        lambda: insert_movement(connection, {**base, "tipo": "INGRESO", "proveniencia_tercero": None}),
    )
    expect_rejected(
        "unvalued USDT cannot claim valuation",
        lambda: insert_movement(connection, {**base, "moneda": "USDT", "tasa_bcv_al_momento": None, "estado_valuacion": "VALUADA"}),
    )
    insert_movement(
        connection,
        {**base, "moneda": "USDT", "tasa_bcv_al_momento": None, "estado_valuacion": "SIN VALUAR"},
    )
    print("PASS unvalued USDT remains representable without a zero rate")
    connection.close()


if __name__ == "__main__":
    main()
