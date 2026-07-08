"""
Script de test 3 : Preuve que la contrainte headcount est stricte.

But : tester avec headcount=1 et headcount=5 pour prouver que
le nombre d'affectations == headcount_needed exactement.

Logique :
  - Appel 1 : headcount=1 -> doit avoir 1 affectation
  - Appel 2 : headcount=5 -> doit avoir 5 affectations
  - Si le bug n'est pas vraiment fixe, les nombres seraient
    differents ou erratiques.
"""

from __future__ import annotations

import httpx

URL = "http://localhost:8001/api/v1/optimize"


def test_headcount(headcount_needed: int) -> tuple[int, str]:
    """Retourne (nombre_affectations, solver_status)."""
    payload = {
        "source": "dataset",
        "projets": [
            {
                "id": f"proj_headcount_{headcount_needed}",
                "name": f"Test Headcount {headcount_needed}",
                "required_skills": ["Python"],
                "required_seniority": None,
                "start_date": "2026-07-01",
                "end_date": "2026-09-30",
                "priority": 3,
                "headcount_needed": headcount_needed,
            }
        ],
    }

    try:
        response = httpx.post(URL, json=payload, timeout=60)
        if response.status_code != 200:
            return -1, f"HTTP {response.status_code}"

        data = response.json()
        num_assignments = len(data.get("plan", []))
        solver_status = data.get("solver_status", "UNKNOWN")
        return num_assignments, solver_status
    except Exception as exc:
        return -1, str(exc)


def main() -> int:
    print("=" * 70)
    print("TEST 3 : Preuve stricte de la contrainte headcount")
    print("=" * 70)
    print()

    test_cases = [
        (1, "Doit affecter exactement 1 collaborateur"),
        (2, "Doit affecter exactement 2 collaborateurs"),
        (3, "Doit affecter exactement 3 collaborateurs"),
        (5, "Doit affecter exactement 5 collaborateurs"),
    ]

    all_ok = True
    for headcount, description in test_cases:
        num_aff, status = test_headcount(headcount)
        matches = num_aff == headcount
        result = "✓ PASS" if matches else "✗ FAIL"

        print(f"{result} | headcount={headcount:2} -> {num_aff:2} affectations | {description}")
        if status != "OPTIMAL" and status != "FEASIBLE":
            print(f"       Statut solveur : {status}")
            all_ok = False
        if not matches:
            print(f"       ERREUR : attendu {headcount}, obtenu {num_aff}")
            all_ok = False

    print()
    if all_ok:
        print(" Conclusion : la contrainte headcount est correctement respectee.")
        print(" Le bug est VRAIMENT fixe.")
        return 0
    else:
        print(" Conclusion : la contrainte headcount n'est PAS stricte.")
        print(" Le bug n'est pas fixe.")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
