import { describe, it, expect } from "vitest";
import {
  CAR_MODELS,
  CAR_MODEL_IDS,
  CAR_MODEL_CATEGORIES,
  listCarModels,
  getCarModel,
  formatCarModelList,
  type CarModel,
} from "./car-models.js";

describe("CAR_MODELS database", () => {
  it("has at least 15 models", () => {
    expect(CAR_MODEL_IDS.length).toBeGreaterThanOrEqual(15);
  });

  it("CAR_MODEL_IDS matches CAR_MODELS keys", () => {
    expect(CAR_MODEL_IDS.sort()).toEqual(Object.keys(CAR_MODELS).sort());
  });

  it("has at least 5 categories represented", () => {
    const categories = new Set(Object.values(CAR_MODELS).map((m) => m.category));
    expect(categories.size).toBeGreaterThanOrEqual(5);
  });

  it("every model has all required fields", () => {
    for (const id of CAR_MODEL_IDS) {
      const model = CAR_MODELS[id];
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
    for (const model of Object.values(CAR_MODELS)) {
      expect(model.sourceUrl).toMatch(/^https?:\/\//);
    }
  });

  it("every model description is at least 20 chars", () => {
    for (const model of Object.values(CAR_MODELS)) {
      expect(model.description.length).toBeGreaterThanOrEqual(20);
    }
  });

  it("has Khronos ToyCar model", () => {
    const toycar = CAR_MODELS["khronos-toycar"];
    expect(toycar).toBeDefined();
    expect(toycar.name).toContain("ToyCar");
    expect(toycar.source).toContain("Khronos");
  });

  it("has Sketchfab models", () => {
    const sf = Object.values(CAR_MODELS).filter((m) =>
      m.source.includes("Sketchfab")
    );
    expect(sf.length).toBeGreaterThanOrEqual(5);
  });

  it("has engine models", () => {
    const engines = Object.values(CAR_MODELS).filter((m) =>
      m.category === "engine"
    );
    expect(engines.length).toBeGreaterThanOrEqual(1);
  });

  it("has interior models", () => {
    const interiors = Object.values(CAR_MODELS).filter((m) =>
      m.category === "interior"
    );
    expect(interiors.length).toBeGreaterThanOrEqual(1);
  });

  it("has complete car models", () => {
    const cars = Object.values(CAR_MODELS).filter((m) =>
      m.category === "complete-car"
    );
    expect(cars.length).toBeGreaterThanOrEqual(3);
  });
});

describe("getCarModel", () => {
  it("returns model by valid ID", () => {
    const model = getCarModel("khronos-toycar");
    expect(model).toBeDefined();
    expect(model!.name).toContain("ToyCar");
  });

  it("returns undefined for unknown ID", () => {
    expect(getCarModel("nonexistent")).toBeUndefined();
  });
});

describe("listCarModels", () => {
  it("returns all models when no filter", () => {
    const models = listCarModels();
    expect(models.length).toBe(CAR_MODEL_IDS.length);
  });

  it("filters by category", () => {
    const engines = listCarModels("engine");
    expect(engines.length).toBeGreaterThan(0);
    for (const m of engines) {
      expect(m.category).toBe("engine");
    }
  });

  it("filters by tag", () => {
    const pbrModels = listCarModels(undefined, "pbr");
    expect(pbrModels.length).toBeGreaterThan(0);
    for (const m of pbrModels) {
      expect(m.tags).toContain("pbr");
    }
  });

  it("filters by both category and tag", () => {
    const result = listCarModels("test-model", "khronos");
    expect(result.length).toBeGreaterThan(0);
    for (const m of result) {
      expect(m.category).toBe("test-model");
      expect(m.tags).toContain("khronos");
    }
  });

  it("returns empty array for non-matching filter", () => {
    const result = listCarModels("engine", "nonexistent-tag");
    expect(result).toEqual([]);
  });
});

describe("formatCarModelList", () => {
  it("formats model list with header and count", () => {
    const models = listCarModels("engine");
    const text = formatCarModelList(models);
    expect(text).toContain("Free Car 3D Models");
    expect(text).toContain(`(${models.length})`);
  });

  it("includes model details in output", () => {
    const models = [getCarModel("khronos-toycar")!];
    const text = formatCarModelList(models);
    expect(text).toContain("ToyCar");
    expect(text).toContain("Khronos");
    expect(text).toContain("CC0");
  });

  it("returns 'no models found' for empty list", () => {
    const text = formatCarModelList([]);
    expect(text).toContain("No car models found");
  });

  it("includes source URLs as links", () => {
    const models = listCarModels("complete-car");
    const text = formatCarModelList(models);
    expect(text).toContain("https://");
  });
});
