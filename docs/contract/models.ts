// Contrato de datos compartido con la app web de Gabriel Pérez.
//
// Copia de referencia para el módulo móvil: NO se compila, solo se lee. La fuente original
// vive en apps/web/src/types/models.ts. Última sincronización: 18/09/2026, commit b08124a
// ("Arreglos mínimos al models.ts para mayor coherencia lógica...").
//
// Los modelos de Kotlin en domain/model deben usar estos mismos nombres de campo: si acá
// cambia algo, hay que reflejarlo del lado móvil o se rompe la paridad web–móvil.

// ─── PERFIL DE USUARIO ────────────────────────────────────────────────────────

export interface UserProfile {
    id: string;
    name: string;
    email: string;
    phone?: string;
    location: string;
    photoUrl?: string;
    bio?: string;
    birthDate?: string;
    // Pérez separó la antigua "availability" en dos listas (18/09): mezclaba jornada con
    // modalidad, y "remote" no es una jornada. Ahora son selección múltiple.
    types: ('full-time' | 'part-time' | 'freelance' | 'internship')[];
    modalities: ('remote' | 'on-site' | 'hybrid')[];
    sectors: string[];             // ej: ["Tecnología", "Diseño"]
}

// ─── CV / CURRÍCULUM ──────────────────────────────────────────────────────────

export interface CV {
    id: string;
    userId: string;
    title: string;                 // nombre que le da el usuario al CV
    templateId: string;
    createdAt: string;             // ISO 8601: "2026-09-13T21:00:00Z"
    updatedAt: string;
    personalInfo: UserProfile;
    workExperiences: WorkExperience[];
    education: Education[];
    skills: Skill[];
    languages: Language[];
}

export interface WorkExperience {
    id: string;
    company: string;
    position: string;
    startDate: string;             // "2024-01"
    endDate: string | null;        // null si es el trabajo actual
    current: boolean;
    description: string;
    // Clave para el módulo de "traducción de experiencia no laboral"
    type: 'formal' | 'freelance' | 'volunteer' | 'school-project' | 'informal';
}

export interface Education {
    id: string;
    institution: string;
    degree: string;
    field: string;
    startDate: string;
    endDate: string | null;        // null si todavía está estudiando
    current: boolean;
}

export interface Skill {
    id: string;
    name: string;
    level: 'beginner' | 'intermediate' | 'advanced' | 'expert';
    category: string;              // ej: "Programación", "Diseño", "Idiomas"
}

export interface Language {
    id: string;
    name: string;
    level: 'basic' | 'intermediate' | 'advanced' | 'native';
}

// ─── PLANTILLAS DE CV ─────────────────────────────────────────────────────────

export interface CVTemplate {
    id: string;
    name: string;
    description: string;
    previewUrl: string;
    category: string;
    isPremium: boolean;
    availableOffline: boolean;
}

// ─── OFERTAS LABORALES ────────────────────────────────────────────────────────

export interface JobOffer {
    id: string;
    title: string;
    company: string;
    companyLogoUrl?: string;       // opcional: el diseño de Samuel lo muestra en la tarjeta
    location: string;
    modality: 'remote' | 'on-site' | 'hybrid';
    type: 'full-time' | 'part-time' | 'freelance' | 'internship';
    sector: string;
    description: string;
    requirements: string[];
    salary?: {
        min: number;
        max: number;
        currency: string;            // "CRC", "USD"
    };
    matchScore?: number;           // 0-100, calculado según el perfil del usuario
    postedAt: string;              // ISO 8601: "2026-09-13T21:00:00Z"
}

// ─── TRACKER DE POSTULACIONES (KANBAN) ────────────────────────────────────────

// Cuatro columnas, según el diseño de Samuel.
export type ApplicationStatus =
    | 'applied'      // Aplicado
    | 'in-progress'  // En proceso
    | 'interview'    // Entrevista
    | 'rejected';    // Rechazado

export interface JobApplication {
    id: string;
    userId: string;
    jobOfferId: string;
    jobTitle: string;              // copia del título por si la oferta se elimina
    company: string;
    companyLogoUrl?: string;
    location: string;
    modality: 'remote' | 'on-site' | 'hybrid';
    status: ApplicationStatus;
    appliedAt: string;             // ISO 8601: "2026-09-13T21:00:00Z"
    updatedAt: string;
    notes?: string;
}

// ─── ANÁLISIS DE BRECHA DE HABILIDADES ───────────────────────────────────────

// Un ítem del radar: nivel del usuario contra lo que pide el mercado.
export interface SkillGapItem {
    name: string;        // ej: "Comunicación"
    userLevel: number;   // 0-100, nivel actual del usuario
    marketLevel: number; // 0-100, lo que pide el mercado
}

// Conversión del nivel categórico de Skill al 0-100 del radar.
// Es una regla compartida: la web y el móvil deben dibujar el mismo gráfico.
export const levelToNumber: Record<Skill['level'], number> = {
    beginner:     25,
    intermediate: 50,
    advanced:     75,
    expert:       100,
};

export interface SkillsGapAnalysis {
    id: string;
    userId: string;
    targetJobTitle: string;
    skills: SkillGapItem[];        // reemplaza missingSkills
    recommendations: CourseRecommendation[];
    createdAt: string;             // ISO 8601: "2026-09-13T21:00:00Z"
}

export interface CourseRecommendation {
    id: string;
    title: string;
    platform: string;              // "YouTube", "Udemy", "Coursera", etc.
    url: string;
    isPaid: boolean;
    duration?: string;             // "4 horas", "6 semanas"
}

// ─── PAGINACIÓN Y ERRORES ─────────────────────────────────────────────────────

// Reemplaza PaginatedResponse<T> en api.ts — nombres exactos del enunciado.
export interface PagedResult<T> {
    items: T[];
    page: number;
    pageSize: number;
    totalRecords: number;          // ojo: totalRecords, no total (lo pide el enunciado)
}

// Respuesta de error de la API — permite pintar el campo exacto del formulario.
export interface ApiErrorResponse {
    status: number;
    message: string;
    errors?: Record<string, string>;  // solo en 400, ej: { "email": "ya está en uso" }
}
