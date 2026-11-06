import assert from "node:assert/strict";
import { mkdir, readFile, stat, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const outputDirectory = fileURLToPath(new URL("../app/src/main/assets/pet/", import.meta.url));
const stageWidth = 1600;
const stageHeight = 800;
const duration = 300;
const frameRate = 30;
const poseFrames = 3;
const runnerParent = 100;
const fixed = (value) => ({ a: 0, k: value });
const animated = (points, { hold = false, linear = false } = {}) => ({
  a: 1,
  k: points.map(([frame, value], index) => {
    const keyframe = { t: frame, s: Array.isArray(value) ? value : [value] };
    if (hold) keyframe.h = 1;
    else if (index < points.length - 1) {
      keyframe.e = Array.isArray(points[index + 1][1]) ? points[index + 1][1] : [points[index + 1][1]];
      keyframe.i = { x: [linear ? 1 : 0.67], y: [1] };
      keyframe.o = { x: [linear ? 0 : 0.33], y: [0] };
    }
    return keyframe;
  }),
});
const transform = ({ position = [0, 0, 0], anchor = [0, 0, 0], scale = [100, 100, 100], rotation = 0, opacity = 100 } = {}) => ({
  o: typeof opacity === "object" ? opacity : fixed(opacity),
  r: typeof rotation === "object" ? rotation : fixed(rotation),
  p: Array.isArray(position) ? fixed(position) : position,
  a: fixed(anchor),
  s: Array.isArray(scale) ? fixed(scale) : scale,
});
const layer = (index, type, name, properties = {}) => ({
  ddd: 0, ind: index, ty: type, nm: name, sr: 1,
  ks: transform(properties), ao: 0, ip: 0, op: duration, st: 0, bm: 0,
});
const path = (vertices, closed = true) => ({
  i: vertices.map(() => [0, 0]), o: vertices.map(() => [0, 0]), v: vertices, c: closed,
});
const shapePath = (vertices, closed = true) => ({
  ty: "sh", d: 1, ks: fixed(path(vertices, closed)), nm: "Open ink path",
});
const fill = (color) => ({ ty: "fl", c: fixed(color), o: fixed(100), r: 1, nm: "Paper pigment" });
const stroke = (color, width) => ({
  ty: "st", c: fixed(color), o: fixed(100), w: fixed(width), lc: 2, lj: 2, nm: "Soft ink line",
});
const ellipse = (size) => ({ ty: "el", d: 1, s: fixed(size), p: fixed([0, 0]), nm: "Round shape" });
const visibility = (ranges) => {
  const points = new Map([[0, 0], [duration, 0]]);
  ranges.forEach(([start, end]) => { points.set(start, 100); points.set(end, 0); });
  return animated([...points].sort(([first], [second]) => first - second), { hold: true });
};

const images = [];
for (const name of ["idle", "sleeping", "analyzing", "complete", "running-sheet"]) {
  const imagePath = `${outputDirectory}/images/${name}.png`;
  assert((await stat(imagePath)).size > 0);
  const bytes = await readFile(imagePath);
  assert.equal(bytes.subarray(1, 4).toString(), "PNG");
  images.push({ id: `cat-${name}`, w: bytes.readUInt32BE(16), h: bytes.readUInt32BE(20), u: "images/", p: `${name}.png`, e: 0 });
}
const sheet = images.find((asset) => asset.id === "cat-running-sheet");
assert.equal(sheet.w % 3, 0);
assert.equal(sheet.h % 2, 0);
const cellWidth = sheet.w / 3;
const cellHeight = sheet.h / 2;
const poseAssets = Array.from({ length: 6 }, (_, index) => {
  const left = (index % 3) * cellWidth;
  const top = Math.floor(index / 3) * cellHeight;
  return {
    id: `run-pose-${index}`, w: cellWidth, h: cellHeight,
    layers: [{
      ...layer(1, 2, `Four-legged gallop pose ${index + 1}`, { position: [-left, -top, 0] }),
      refId: sheet.id,
      masksProperties: [{
        inv: false, mode: "a", nm: `Sprite cell ${index + 1}`,
        pt: fixed(path([[left, top], [left + cellWidth, top], [left + cellWidth, top + cellHeight], [left, top + cellHeight]])),
        o: fixed(100), x: fixed(0),
      }],
    }],
  };
});

const routeDefinitions = [
  {
    name: "analyzing", label: "Energetic link search",
    route: [[0, 800], [35, 1320], [105, 280], [175, 1320], [210, 800], [300, 800]],
    directions: [[0, 1], [35, -1], [105, 1], [175, -1], [300, -1]],
    runs: [[0, 210]],
    stops: [{ start: 210, end: 300, x: 800, image: "analyzing" }],
  },
  {
    name: "complete", label: "Found it celebration",
    route: [[0, 800]],
    directions: [[0, 1]],
    runs: [],
    stops: [{ start: 0, end: 300, x: 800, image: "complete" }],
  },
  {
    name: "error", label: "Turn back and try again",
    route: [[0, 800], [45, 1230], [60, 1230], [105, 800], [300, 800]],
    directions: [[0, 1], [60, -1], [300, -1]],
    runs: [[0, 45], [60, 105]],
    stops: [{ start: 45, end: 60, x: 1230, image: "idle" }, { start: 105, end: 300, x: 800, image: "idle" }],
  },
];

const runnerLayers = (definition) => {
  const frameTimes = [...new Set([
    ...Array.from({ length: duration / poseFrames + 1 }, (_, index) => index * poseFrames),
    ...definition.runs.flat(),
  ])].sort((first, second) => first - second);
  const poseLayers = poseAssets.map((asset, poseIndex) => {
    const opacity = [];
    const bounce = [];
    for (const frame of frameTimes) {
      const phase = Math.floor(frame / poseFrames) % 6;
      const running = definition.runs.some(([start, end]) => frame >= start && frame < end);
      opacity.push([frame, running && phase === poseIndex ? 100 : 0]);
      bounce.push([frame, [0, [-14, 0, 8, -8, -18, -12][phase], 0]]);
    }
    return {
      ...layer(poseIndex + 1, 0, `Gallop limb cycle ${poseIndex + 1}`, {
        position: animated(bounce), anchor: [cellWidth / 2, cellHeight / 2, 0],
        scale: [110, 110, 100], opacity: animated(opacity, { hold: true }),
      }),
      parent: runnerParent, refId: asset.id, w: cellWidth, h: cellHeight,
    };
  });
  const parent = {
    ...layer(runnerParent, 3, "Running cat travel and turnarounds", {
      position: animated(definition.route.map(([frame, horizontal]) => [frame, [horizontal, 440, 0]]), { linear: true }),
      scale: animated(definition.directions.map(([frame, direction]) => [frame, [direction * 100, 100, 100]]), { hold: true }),
      opacity: visibility(definition.runs),
    }),
  };
  return [...poseLayers, parent];
};
const stopLayer = (stop, index, state) => {
  const image = images.find((asset) => asset.id === `cat-${stop.image}`);
  const scale = 38;
  const entrance = Math.min(stop.start + 8, stop.end - 2);
  const settle = Math.min(stop.start + 14, stop.end - 1);
  const points = [[stop.start, [stop.x, 440, 0]], [entrance, [stop.x, state === "complete" ? 395 : 432, 0]], [settle, [stop.x, 440, 0]]];
  if (state === "complete" && stop.end - stop.start > 60) {
    for (const peak of [26, 62, 98]) {
      if (peak + 10 < stop.end) {
        points.push([stop.start + peak, [stop.x, 395, 0]], [stop.start + peak + 10, [stop.x, 440, 0]]);
      }
    }
  }
  points.push([stop.end, [stop.x, 440, 0]]);
  const output = {
    ...layer(200 + index, 2, `Stop, face viewer and ${stop.image}`, {
      position: animated(points), anchor: [image.w / 2, image.h / 2, 0],
      scale: animated([[stop.start, [scale * 0.9, scale * 1.05, 100]], [entrance, [scale * 1.04, scale * 0.97, 100]], [settle, [scale, scale, 100]], [stop.end, [scale, scale, 100]]]),
      rotation: state === "error" ? animated([[stop.start, 0], [settle, -8], [stop.end - 12, -8], [stop.end, 0]]) : 0,
    }),
    refId: image.id, ip: stop.start, op: stop.end,
  };
  return output;
};
const dustLayers = (definition) => Array.from({ length: 4 }, (_, index) => {
  const opacity = [];
  const position = [];
  const size = [];
  for (let frame = 0; frame <= duration; frame += 3) {
    const phase = ((frame + index * 4) % 24) / 24;
    const running = definition.runs.some(([start, end]) => frame >= start && frame < end);
    opacity.push([frame, running ? (1 - phase) * 34 : 0]);
    position.push([frame, [-170 - phase * 130, 192 - phase * 38 + index * 5, 0]]);
    size.push([frame, [40 + phase * 65, 40 + phase * 65, 100]]);
  }
  return {
    ...layer(300 + index, 4, `Soft running dust ${index + 1}`, {
      position: animated(position), scale: animated(size), opacity: animated(opacity),
    }),
    parent: runnerParent,
    shapes: [ellipse([26 + index * 4, 18 + index * 3]), fill([0.72, 0.58, 0.44, 1])],
  };
});
const sparkle = (index, start, end, position) => ({
  ...layer(400 + index, 4, `Happy sparkle ${index + 1}`, {
    position: [...position, 0],
    rotation: animated([[start, -20], [start + 10, 0], [end, 20]]),
    opacity: animated([[start, 0], [start + 10, 100], [end, 0]]),
    scale: animated([[start, [20, 20, 100]], [start + 10, [100, 100, 100]], [end, [20, 20, 100]]]),
  }),
  ip: start, op: end,
  shapes: [
    { ty: "sr", sy: 1, d: 1, pt: fixed(4), p: fixed([0, 0]), r: fixed(0), or: fixed(35), os: fixed(0), ir: fixed(8), is: fixed(0), nm: "Four point star" },
    fill([1, 0.74, 0.32, 1]),
  ],
});
const searchDot = (index) => {
  const opacity = [];
  for (let frame = 210; frame <= duration; frame += 6) opacity.push([frame, ((frame - 210 + index * 6) % 30) < 12 ? 100 : 20]);
  return {
    ...layer(500 + index, 4, `Thinking dot ${index + 1}`, { position: [744 + index * 56, 718, 0], opacity: animated(opacity) }),
    ip: 210, shapes: [ellipse([18, 18]), fill([0.85, 0.42, 0.34, 1])],
  };
};
const sleepZPath = shapePath([[-20, -22], [20, -22], [-20, 22], [20, 22]], false);
const sleepingImage = images.find((asset) => asset.id === "cat-sleeping");
const sleepPosition = [];
const sleepScale = [];
const sleepShadowOpacity = [];
for (let frame = 0; frame <= duration; frame += 5) {
  const cycle = (frame / duration) * Math.PI * 4;
  const inhale = (Math.sin(cycle - Math.PI / 2) + 1) / 2;
  sleepPosition.push([frame, [800, 444 - inhale * 5, 0]]);
  sleepScale.push([frame, [40 + inhale * 0.35, 39.2 + inhale * 0.7, 100]]);
  sleepShadowOpacity.push([frame, [frame === 0 ? 8 : 8 + inhale * 3]]);
}
const sleepShadow = {
  ...layer(250, 4, "Soft sleeping floor shadow", {
    position: [800, 620, 0],
    opacity: animated(sleepShadowOpacity),
  }),
  shapes: [ellipse([700, 48]), fill([0.32, 0.23, 0.16, 1])],
};
const sleepCat = {
  ...layer(260, 2, "Curled-up Xiaohei breathing in place", {
    position: animated(sleepPosition),
    anchor: [sleepingImage.w / 2, sleepingImage.h / 2, 0],
    scale: animated(sleepScale),
  }),
  refId: sleepingImage.id,
};
const sleepZ = (index) => {
  const opacity = [];
  const position = [];
  const scale = [];
  const rotation = [];
  for (let frame = 0; frame <= duration; frame += 5) {
    const phase = ((frame + index * 30) % 120) / 120;
    const fade = Math.pow(Math.sin(Math.PI * phase), 1.35);
    opacity.push([frame, fade * 72]);
    position.push([frame, [914 + index * 48 + phase * 34, 318 - phase * 108, 0]]);
    scale.push([frame, [56 + phase * 38, 56 + phase * 38, 100]]);
    rotation.push([frame, -8 + phase * 16]);
  }
  return {
    ...layer(400 + index, 4, `Floating sleep Z ${index + 1}`, {
      position: animated(position),
      scale: animated(scale),
      rotation: animated(rotation),
      opacity: animated(opacity),
    }),
    shapes: [sleepZPath, stroke([0.94, 0.49, 0.39, 1], 7)],
  };
};

await mkdir(outputDirectory, { recursive: true });
for (const definition of routeDefinitions) {
  const overlays = definition.name === "complete"
    ? Array.from({ length: 12 }, (_, index) => {
      const start = 52 + index * 15;
      const side = index % 2 === 0 ? -1 : 1;
      return sparkle(index, start, start + 35, [800 + side * (230 + index % 3 * 48), 320 + index % 4 * 80]);
    })
    : definition.name === "analyzing" ? [0, 1, 2].map(searchDot) : [];
  const layers = [
    ...overlays,
    ...definition.stops.map((stop, index) => stopLayer(stop, index, definition.name)),
    ...runnerLayers(definition),
    ...dustLayers(definition),
  ];
  const outputDuration = definition.name === "complete" ? 150 : duration;
  const animation = {
    v: "5.12.2", fr: frameRate, ip: 0, op: outputDuration,
    w: stageWidth, h: stageHeight, nm: `Xiaohei cartoon — ${definition.label}`, ddd: 0,
    assets: [...images, ...poseAssets], layers,
    markers: definition.stops.map((stop) => ({
      tm: stop.start,
      cm: `Face viewer: ${stop.image}`,
      dr: Math.min(stop.end, outputDuration) - stop.start,
    })),
  };
  assert.equal(new Set(layers.map((entry) => entry.ind)).size, layers.length);
  assert.equal(poseAssets.length, 6);
  assert(definition.route.every(([, horizontal]) => horizontal >= 260 && horizontal <= 1340));
  assert(definition.stops.every((stop) => stop.end > stop.start));
  for (let frame = 0; frame < duration; frame += 1) {
    const running = definition.runs.some(([start, end]) => frame >= start && frame < end);
    const activePoses = layers.filter((entry) => entry.nm.startsWith("Gallop limb cycle")).filter((entry) => {
      const keyframe = entry.ks.o.k.findLast((point) => point.t <= frame);
      return keyframe.s[0] > 0;
    });
    const activeStops = definition.stops.filter((stop) => frame >= stop.start && frame < stop.end);
    assert.equal(activePoses.length, running ? 1 : 0, `${definition.name} has overlapping run poses at frame ${frame}`);
    assert.equal(activePoses.length + activeStops.length, 1, `${definition.name} has overlapping or missing cats at frame ${frame}`);
  }
  await writeFile(`${outputDirectory}/${definition.name}.json`, `${JSON.stringify(animation)}\n`);
}
await writeFile(`${outputDirectory}/idle.json`, `${JSON.stringify({
  v: "5.12.2", fr: frameRate, ip: 0, op: duration,
  w: stageWidth, h: stageHeight, nm: "Xiaohei cartoon — Sleeping in place", ddd: 0,
  assets: images,
  layers: [sleepZ(0), sleepZ(1), sleepZ(2), sleepCat, sleepShadow],
  markers: [{ tm: 0, cm: "Sleep in place", dr: duration }],
})}\n`);
console.log(`Generated ${routeDefinitions.length + 1} cartoon timelines with sleep, gallop, celebration and error beats.`);
