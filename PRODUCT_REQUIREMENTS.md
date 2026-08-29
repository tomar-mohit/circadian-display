# Product Requirements

## MVP Features

### Curve Engine

Users can create multiple curve points.

Each curve point contains:

* Time
* Warmth value
* Dimming value

The system interpolates between points.

### Modes

#### Native Mode

Uses Android-native display adjustment APIs where available.

#### Overlay Mode

Uses an overlay to provide advanced warmth and dimming controls.

### Scheduler

Applies curve values automatically based on current time.

### Excluded Apps

Users can disable adjustments for specific applications.

### Preview Mode

Users can simulate any time of day and preview resulting adjustments.

## Non Goals

Version 1 will NOT include:

* AI features
* Sleep tracking
* Wearable integration
* User accounts
* Cloud synchronization
* Social features
* Telemetry
