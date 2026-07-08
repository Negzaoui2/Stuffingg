"""
Script de test 2 : Optimisation en mode dataset.

But : verifier que le DatasetProvider lit bien le CSV local,
et que POST /api/v1/optimize fonctionne avec source='dataset'.

Pre-requis : optimization-engine doit tourner sur http://localhost:8001
"""

from __future__ import annotations

import httpx

from app.services.data_provider import DatasetProvider

URL = "http://localhost:8001/api/v1/optimize"


def main() -> int:
    print("=" * 70)
    print("TEST 2 : Mode dataset (DataProvider + API)")
    print("=" * 70)

    # 1) Test direct du DatasetProvider
    try:
        provider = DatasetProvider()
        collaborateurs = provider.get_collaborateurs()
        print(f"DatasetProvider OK : {len(collaborateurs)} collaborateurs charges")
        if collaborateurs:
            c0 = collaborateurs[0]
            print(
                "Exemple collaborateur : "
                f"id={c0.id}, name={c0.name}, seniority={c0.seniority}, "
                f"skills={len(c0.skills)}"
            )
    except Exception as exc:
        print(f"ECHEC DatasetProvider : {type(exc).__name__}: {exc}")
        return 1

    # 2) Test API /optimize avec source=dataset
    payload = {
        "source": "dataset",
        "projets": [
            {
                "id": "proj_dataset_001",
                "name": "Projet Data Provider Validation",
                "required_skills": ["Python", "SQL"],
                "required_seniority": "mid",
                "start_date": "2026-07-01",
                "end_date": "2026-09-30",
                "priority": 4,
                "headcount_needed": 3,
            }
        ],
        "weights": {
            "skills_match": 0.4,
            "cost": 0.3,
            "availability": 0.2,
            "balance": 0.1,
        },
    }

    print()
    print(f"POST {URL}")
    try:
        response = httpx.post(URL, json=payload, timeout=60)
    except httpx.ConnectError:
        print("ECHEC - impossible de se connecter au service.")
        print("Demarrer le serveur : uvicorn app.main:app --reload --port 8001")
        return 1

    print(f"Status HTTP : {response.status_code}")
    if response.status_code != 200:
        print("ECHEC - reponse non-200")
        print("Body :", response.text)
        return 1

    data = response.json()
    plan = data.get("plan", [])

    print("SUCCES - reponse recue")
    print(f"Nombre d'affectations : {len(plan)}")
    print(f"Statut solveur : {data.get('solver_status')}")
    print(f"Score global : {data.get('global_score')}")
    print(f"Metriques : {data.get('metrics')}")

    if plan:
        print("--- Apercu plan (5 premieres lignes) ---")
        for aff in plan[:5]:
            print(
                f"  - {aff['collaborateur_name']} -> {aff['projet_name']} "
                f"a {aff['allocation_percentage']}% "
                f"(skills={aff.get('skills_match_score', 'N/A')})"
            )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
