"""
Script de test 1 : Optimisation avec donnees fictives.

But : verifier que POST /api/v1/optimize fonctionne avec un payload simple.
Aucune dependance externe (pas de Spring Boot, pas de CSV).

Pre-requis : optimization-engine doit tourner sur http://localhost:8001
"""

from __future__ import annotations

import sys

import httpx

URL = "http://localhost:8001/api/v1/optimize"

# Le payload est aligne avec app/models/schemas.py
payload = {
    "source": "postgres",  # mode "donnees fournies par l'appelant"
    "projets": [
        {
            "id": "1",
            "name": "Refonte API Sopra HR",
            "required_skills": ["Java", "Spring Boot"],
            "required_seniority": "Senior",
            "start_date": "2026-07-01",
            "end_date": "2026-12-31",
            "priority": 5,
            "headcount_needed": 2,
        }
    ],
    "collaborateurs": [
        {
            "id": "101",
            "name": "Alice Martin",
            "department": "IT",
            "skills": ["Java", "Spring Boot", "PostgreSQL"],
            "seniority": "Senior",
            "daily_cost": 800,
            "available_days": 220,
            "absence_days_last_12_months": 0,
            "main_absence_type": None,
            "current_fte": 0.0,
        },
        {
            "id": "102",
            "name": "Bob Dupont",
            "department": "IT",
            "skills": ["Java", "Spring Boot"],
            "seniority": "Senior",
            "daily_cost": 750,
            "available_days": 176,
            "absence_days_last_12_months": 12,
            "main_absence_type": "vacation",
            "current_fte": 0.0,
        },
        {
            "id": "103",
            "name": "Chloe Bernard",
            "department": "IT",
            "skills": ["Python", "Django"],
            "seniority": "Junior",
            "daily_cost": 500,
            "available_days": 220,
            "absence_days_last_12_months": 3,
            "main_absence_type": "vacation",
            "current_fte": 0.0,
        },
        {
            "id": "104",
            "name": "David Petit",
            "department": "IT",
            "skills": ["Java"],
            "seniority": "Junior",
            "daily_cost": 450,
            "available_days": 220,
            "absence_days_last_12_months": 0,
            "main_absence_type": None,
            "current_fte": 0.0,
        },
    ],
    "weights": {
        "skills_match": 0.5,
        "cost": 0.2,
        "availability": 0.2,
        "balance": 0.1,
    },
}


def main() -> int:
    print("=" * 70)
    print("TEST 1 : Donnees fictives - POST /api/v1/optimize")
    print("=" * 70)
    print(f"URL : {URL}")
    print(f"Projets : {len(payload['projets'])}")
    print(f"Collaborateurs : {len(payload['collaborateurs'])}")
    print()

    try:
        response = httpx.post(URL, json=payload, timeout=60)
        print(f"Status HTTP : {response.status_code}")
        print()

        if response.status_code != 200:
            print("ECHEC - reponse non-200")
            print("Body :", response.text)
            return 1

        data = response.json()
        print("SUCCES - reponse recue")
        print()
        print("--- Plan d'affectation ---")
        for aff in data.get("plan", []):
            print(
                f"  - {aff['collaborateur_name']} (id={aff['collaborateur_id']}) "
                f"-> {aff['projet_name']} "
                f"a {aff['allocation_percentage']}% "
                f"(skills match: {aff.get('skills_match_score', 'N/A')})"
            )

        print()
        print("--- Metriques globales ---")
        print(f"  Score global : {data.get('global_score')}")
        print(f"  Statut solveur : {data.get('solver_status')}")
        print(f"  Temps de calcul : {data.get('computation_time_ms')} ms")
        print(f"  Details metriques : {data.get('metrics')}")

        print()
        print("Verifications attendues :")
        print("  - Alice et Bob devraient etre favorises (Java + Spring Boot)")
        print("  - Chloe devrait etre penalisee (pas Java)")
        print("  - David peut etre affecte (Java seul)")
        print("  - solver_status devrait etre OPTIMAL ou FEASIBLE")
        return 0

    except httpx.ConnectError:
        print("ECHEC - impossible de se connecter a l'optimization-engine")
        print("Verifier qu'il tourne sur le port 8001 :")
        print("uvicorn app.main:app --reload --port 8001")
        return 1
    except Exception as exc:  # pragma: no cover - script manuel
        print(f"ECHEC inattendu : {type(exc).__name__}: {exc}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
