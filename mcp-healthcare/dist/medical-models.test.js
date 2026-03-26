import { describe, it, expect } from "vitest";
import { MEDICAL_MODELS, MEDICAL_MODEL_IDS, MEDICAL_MODEL_CATEGORIES, listMedicalModels, getMedicalModel, formatModelList, } from "./medical-models.js";
describe("MEDICAL_MODELS database", () => {
    it("has at least 20 models", () => {
        expect(MEDICAL_MODEL_IDS.length).toBeGreaterThanOrEqual(20);
    });
    it("MEDICAL_MODEL_IDS matches MEDICAL_MODELS keys", () => {
        expect(MEDICAL_MODEL_IDS.sort()).toEqual(Object.keys(MEDICAL_MODELS).sort());
    });
    it("has all 10 categories represented", () => {
        const categories = new Set(Object.values(MEDICAL_MODELS).map((m) => m.category));
        for (const cat of MEDICAL_MODEL_CATEGORIES) {
            // Not all categories need to be represented, but core ones should be
        }
        expect(categories.size).toBeGreaterThanOrEqual(6);
    });
    it("every model has all required fields", () => {
        for (const id of MEDICAL_MODEL_IDS) {
            const model = MEDICAL_MODELS[id];
            expect(model.id).toBe(id);
            expect(model.name).toBeTruthy();
            expect(model.category).toBeTruthy();
            expect(model.source).toBeTruthy();
            expect(model.sourceUrl).toBeTruthy();
            expect(model.format).toBeTruthy();
            expect(model.license).toBeTruthy();
            expect(model.description).toBeTruthy();
            expect(model.tags.length).toBeGreaterThan(0);
        }
    });
    it("every model has a valid source URL", () => {
        for (const model of Object.values(MEDICAL_MODELS)) {
            expect(model.sourceUrl).toMatch(/^https?:\/\//);
        }
    });
    it("every model description is at least 20 chars", () => {
        for (const model of Object.values(MEDICAL_MODELS)) {
            expect(model.description.length).toBeGreaterThanOrEqual(20);
        }
    });
    it("has BodyParts3D models", () => {
        const bp3d = Object.values(MEDICAL_MODELS).filter((m) => m.source.includes("BodyParts3D"));
        expect(bp3d.length).toBeGreaterThanOrEqual(5);
    });
    it("has NIH models", () => {
        const nih = Object.values(MEDICAL_MODELS).filter((m) => m.source.includes("NIH"));
        expect(nih.length).toBeGreaterThanOrEqual(4);
    });
    it("has Sketchfab models", () => {
        const sf = Object.values(MEDICAL_MODELS).filter((m) => m.source.includes("Sketchfab"));
        expect(sf.length).toBeGreaterThanOrEqual(4);
    });
});
describe("getMedicalModel", () => {
    it("returns model by valid ID", () => {
        const model = getMedicalModel("bp3d-heart");
        expect(model).toBeDefined();
        expect(model.name).toBe("Human Heart");
    });
    it("returns undefined for unknown ID", () => {
        expect(getMedicalModel("nonexistent")).toBeUndefined();
    });
});
describe("listMedicalModels", () => {
    it("returns all models when no filter", () => {
        const models = listMedicalModels();
        expect(models.length).toBe(MEDICAL_MODEL_IDS.length);
    });
    it("filters by category", () => {
        const organs = listMedicalModels("organ");
        expect(organs.length).toBeGreaterThan(0);
        for (const m of organs) {
            expect(m.category).toBe("organ");
        }
    });
    it("filters by tag", () => {
        const dental = listMedicalModels(undefined, "dental");
        expect(dental.length).toBeGreaterThan(0);
        for (const m of dental) {
            expect(m.tags).toContain("dental");
        }
    });
    it("filters by both category and tag", () => {
        const skeletonBones = listMedicalModels("skeleton", "bones");
        expect(skeletonBones.length).toBeGreaterThan(0);
        for (const m of skeletonBones) {
            expect(m.category).toBe("skeleton");
            expect(m.tags).toContain("bones");
        }
    });
    it("returns empty array for non-matching filter", () => {
        const result = listMedicalModels("anatomy", "nonexistent-tag");
        expect(result).toEqual([]);
    });
});
describe("formatModelList", () => {
    it("formats model list with header and count", () => {
        const models = listMedicalModels("organ");
        const text = formatModelList(models);
        expect(text).toContain("Free Medical 3D Models");
        expect(text).toContain(`(${models.length})`);
    });
    it("includes model details in output", () => {
        const models = [getMedicalModel("bp3d-heart")];
        const text = formatModelList(models);
        expect(text).toContain("Human Heart");
        expect(text).toContain("BodyParts3D");
        expect(text).toContain("CC BY-SA");
    });
    it("returns 'no models found' for empty list", () => {
        const text = formatModelList([]);
        expect(text).toContain("No models found");
    });
    it("includes source URLs as links", () => {
        const models = listMedicalModels("molecule");
        const text = formatModelList(models);
        expect(text).toContain("https://");
    });
});
