import time

from ortools.sat.python import cp_model

from app.models.schemas import (
    Affectation,
    Collaborateur,
    OptimizationResponse,
    OptimizationWeights,
    Projet,
)


class StaffingOptimizer:
    """Solveur CP-SAT pour l'affectation optimale collaborateurs/projets."""

    def __init__(
        self,
        collaborateurs: list[Collaborateur],
        projets: list[Projet],
        weights: OptimizationWeights | None = None,
    ):
        self.collaborateurs = collaborateurs
        self.projets = projets
        self.weights = weights or OptimizationWeights()

    def _compute_skills_score(self, collab: Collaborateur, projet: Projet) -> float:
        """Calcule le score de matching skills entre un collaborateur et un projet."""
        if not projet.required_skills:
            return 1.0
        collab_skills = set(s.lower() for s in collab.skills)
        required_skills = set(s.lower() for s in projet.required_skills)
        if not required_skills:
            return 1.0
        matched = collab_skills & required_skills
        return len(matched) / len(required_skills)

    def _check_seniority(self, collab: Collaborateur, projet: Projet) -> bool:
        """Vérifie si le seniority du collaborateur est suffisant."""
        if not projet.required_seniority:
            return True
        levels = {"junior": 1, "mid": 2, "senior": 3, "lead": 4}
        collab_level = levels.get((collab.seniority or "junior").lower(), 1)
        required_level = levels.get(projet.required_seniority.lower(), 1)
        return collab_level >= required_level

    def _compute_availability_score(self, collab: Collaborateur) -> int:
        """Score de disponibilité (0-100) en tenant compte des absences/congés."""
        base_score = min(100, int(max(0, collab.available_days) / 220 * 100))

        # Pénalité liée au volume d'absence sur 12 mois.
        absence_days = max(0, collab.absence_days_last_12_months)
        absence_penalty = int(min(35, (absence_days / 220) * 100 * 0.35))

        # Pénalité légère selon le type d'absence principal.
        absence_type = (collab.main_absence_type or "").strip().lower()
        type_penalty_map = {
            "maladie": 12,
            "sick leave": 12,
            "long leave": 15,
            "conge longue duree": 15,
            "maternite": 8,
            "maternity": 8,
            "vacation": 2,
            "conge annuel": 2,
        }
        type_penalty = type_penalty_map.get(absence_type, 0)

        return max(0, min(100, base_score - absence_penalty - type_penalty))

    def solve(self) -> OptimizationResponse:
        """Résout le problème d'affectation avec CP-SAT."""
        start_time = time.time()

        model = cp_model.CpModel()
        num_collabs = len(self.collaborateurs)
        num_projets = len(self.projets)

        if num_collabs == 0 or num_projets == 0:
            return OptimizationResponse(
                plan=[],
                global_score=0.0,
                metrics={"tnf_score": 0, "skills_score": 0, "cost_score": 0, "balance_score": 0},
                solver_status="NO_DATA",
                computation_time_ms=0.0,
            )

        # Variables de décision: x[i][j] = allocation (0-100%) du collaborateur i au projet j
        # On utilise des paliers de 20% (0, 20, 40, 60, 80, 100)
        x = {}
        for i in range(num_collabs):
            for j in range(num_projets):
                x[i, j] = model.new_int_var(0, 5, f"x_{i}_{j}")  # 0-5 paliers de 20%

        # Contrainte FTE max: somme des allocations par collaborateur <= 5 (100%)
        for i in range(num_collabs):
            model.add(sum(x[i, j] for j in range(num_projets)) <= 5)

        # Contrainte headcount: chaque projet doit avoir exactement headcount_needed collaborateurs affectés
        for j in range(num_projets):
            projet = self.projets[j]
            required_headcount = max(0, projet.headcount_needed)
            # Un collaborateur est "affecté" s'il a au moins 1 palier (20%)
            assigned = []
            for i in range(num_collabs):
                b = model.new_bool_var(f"assigned_{i}_{j}")
                model.add(x[i, j] >= 1).only_enforce_if(b)
                model.add(x[i, j] == 0).only_enforce_if(b.negated())
                assigned.append(b)
            model.add(sum(assigned) == required_headcount)

        # Pré-calcul des scores skills (multiplié par 100 pour rester en entiers)
        skills_scores = {}
        for i in range(num_collabs):
            for j in range(num_projets):
                score = self._compute_skills_score(self.collaborateurs[i], self.projets[j])
                seniority_ok = self._check_seniority(self.collaborateurs[i], self.projets[j])
                if not seniority_ok:
                    score *= 0.3  # Pénalité seniority insuffisant
                skills_scores[i, j] = int(score * 100)

        # Pré-calcul des scores de coût (inversé: moins cher = meilleur score)
        max_cost = max((c.daily_cost for c in self.collaborateurs), default=1)
        cost_scores = {}
        for i in range(num_collabs):
            cost_scores[i] = int((1 - self.collaborateurs[i].daily_cost / max_cost) * 100) if max_cost > 0 else 50

        # Pré-calcul des scores de disponibilité en tenant compte des absences/congés
        availability_scores = {}
        for i in range(num_collabs):
            availability_scores[i] = self._compute_availability_score(self.collaborateurs[i])

        # Objectif: maximiser le score global pondéré
        objective_terms = []
        w_skills = int(self.weights.skills_match * 100)
        w_cost = int(self.weights.cost * 100)
        w_availability = int(self.weights.availability * 100)

        for i in range(num_collabs):
            for j in range(num_projets):
                # Priorité projet: plus le projet est prioritaire, plus son utilité est favorisée.
                priority_weight = max(1, min(5, self.projets[j].priority)) * 20
                # Score skills pondéré
                objective_terms.append(x[i, j] * skills_scores[i, j] * w_skills * priority_weight)
                # Score coût pondéré
                objective_terms.append(x[i, j] * cost_scores[i] * w_cost * priority_weight)
                # Score disponibilité pondéré
                objective_terms.append(x[i, j] * availability_scores[i] * w_availability * priority_weight)

        model.maximize(sum(objective_terms))

        # Résolution
        solver = cp_model.CpSolver()
        solver.parameters.max_time_in_seconds = 30.0
        status = solver.solve(model)

        computation_time = (time.time() - start_time) * 1000

        status_name = {
            cp_model.OPTIMAL: "OPTIMAL",
            cp_model.FEASIBLE: "FEASIBLE",
            cp_model.INFEASIBLE: "INFEASIBLE",
            cp_model.MODEL_INVALID: "MODEL_INVALID",
            cp_model.UNKNOWN: "UNKNOWN",
        }.get(status, "UNKNOWN")

        if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
            return OptimizationResponse(
                plan=[],
                global_score=0.0,
                metrics={"tnf_score": 0, "skills_score": 0, "cost_score": 0, "balance_score": 0},
                solver_status=status_name,
                computation_time_ms=computation_time,
            )

        # Extraction des résultats
        plan = []
        total_skills_score = 0.0
        total_cost_score = 0.0
        total_alloc = 0
        total_availability_score = 0.0

        for i in range(num_collabs):
            for j in range(num_projets):
                val = solver.value(x[i, j])
                if val > 0:
                    alloc_pct = val * 20
                    skill_score = skills_scores[i, j] / 100.0
                    plan.append(
                        Affectation(
                            collaborateur_id=self.collaborateurs[i].id,
                            collaborateur_name=self.collaborateurs[i].name,
                            projet_id=self.projets[j].id,
                            projet_name=self.projets[j].name,
                            allocation_percentage=alloc_pct,
                            skills_match_score=round(skill_score, 2),
                            period=f"{self.projets[j].start_date or 'N/A'} / {self.projets[j].end_date or 'N/A'}",
                        )
                    )
                    total_skills_score += skill_score
                    total_cost_score += cost_scores[i] / 100.0
                    total_availability_score += availability_scores[i] / 100.0
                    total_alloc += 1

        # Calcul des métriques agrégées
        n = max(total_alloc, 1)
        skills_score_avg = round((total_skills_score / n) * 100, 1)
        cost_score_avg = round((total_cost_score / n) * 100, 1)
        availability_score_avg = round((total_availability_score / n) * 100, 1)

        # Score d'équilibre: mesure la répartition de la charge
        alloc_per_collab = [0] * num_collabs
        for i in range(num_collabs):
            for j in range(num_projets):
                alloc_per_collab[i] += solver.value(x[i, j]) * 20
        non_zero = [a for a in alloc_per_collab if a > 0]
        if non_zero:
            mean_alloc = sum(non_zero) / len(non_zero)
            variance = sum((a - mean_alloc) ** 2 for a in non_zero) / len(non_zero)
            balance_score = round(max(0, 100 - variance**0.5), 1)
        else:
            balance_score = 0.0

        # TNF (taux de non-facturation) inversé en score: 100 = aucune capacité inutilisée.
        total_capacity_pct = num_collabs * 100
        used_capacity_pct = sum(alloc_per_collab)
        utilization_pct = (used_capacity_pct / total_capacity_pct * 100) if total_capacity_pct > 0 else 0.0
        tnf_score = round(max(0.0, min(100.0, utilization_pct)), 1)

        global_score = round(
            skills_score_avg * self.weights.skills_match
            + cost_score_avg * self.weights.cost
            + availability_score_avg * self.weights.availability
            + balance_score * self.weights.balance,
            1,
        )

        # Trier par score de matching décroissant
        plan.sort(key=lambda a: a.skills_match_score, reverse=True)

        return OptimizationResponse(
            plan=plan,
            global_score=global_score,
            metrics={
                "tnf_score": tnf_score,
                "skills_score": skills_score_avg,
                "cost_score": cost_score_avg,
                "availability_score": availability_score_avg,
                "balance_score": balance_score,
            },
            solver_status=status_name,
            computation_time_ms=round(computation_time, 2),
        )
