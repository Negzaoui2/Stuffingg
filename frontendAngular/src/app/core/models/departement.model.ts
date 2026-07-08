/** Département tel que retourné par l'API admin */
export interface Departement {
  id: number;
  name: string;
  employeeCount: number;
}

/** Payload pour la création / modification d'un département */
export interface CreateDepartementRequest {
  name: string;
}
