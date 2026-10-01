---
name: data-viz-style
description: Use when drawing a chart or a calendar mark in Gains. One ink line, no gradient fill, one empty-state sentence. A personal-record point is signal red.
---

# Charts and marks

Color comes from `app-color-system`. Type for axis labels is Space Mono, from `ios-style-ui`.

## Line

One series. A thin ink stroke on the paper or canvas. No area fill and no second semantic color for whey, creatine, or a category. The endpoint dot uses the same ink as the line so it stays visible on a dark canvas. A personal-record point is signal red. An estimated one-rep max is not drawn as that point.

Axes are sparse: a few values, no box, no heavy grid. Animate the line drawing once per data load, ease-out, inside the 150–250ms range. Do not replay the draw on an unrelated recompose.

## Empty

When there is not enough data, show one sentence in the label color. Do not draw a fake series, a rainbow placeholder, or a progress ring. Nutrition rings are out of scope.

## Calendar

A logged workout is a filled circle. A personal record on that day may use signal red. A missed due day is a hollow hairline circle. A rest day is the numeral only. Do not paint missed days, overdue days, or an empty week in red.
