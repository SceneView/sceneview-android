# E-commerce Jetpack Compose SceneView Sample App

## Overview

Welcome to the "E-commerce Jetpack Compose SceneView Sample App." This advanced sample app demonstrates how to create a modern e-commerce product description screen with additional features like adding products to the cart and viewing them in your space using SceneView for 3D model placement. The app follows modern Android architecture principles using Clean Architecture.

## Key Components

### Clean Architecture

This app follows Clean Architecture principles, separating the codebase into three layers:

1. **Data Layer**: Responsible for interacting with data sources such as databases, APIs, or local storage. It provides data to the domain layer and includes repository implementations.

2. **Domain Layer**: Contains the business logic and use cases of the app. It is independent of the data and presentation layers and defines the core functionality of the app.

3. **Presentation Layer**: Handles the UI logic and rendering using Jetpack Compose. It communicates with the domain layer via ViewModels to display data and respond to user interactions.

### UI Events

UI events in this app represent user interactions or actions triggered within the Composable functions. These events are sent to the ViewModel for processing. For example, when the user taps the "Add to Cart" button or "View in your space" button, UI events are dispatched to the ViewModel to handle the corresponding actions.

### ViewModel

The ViewModel is a crucial part of this app's architecture. It acts as an intermediary between the Presentation and Domain layers. ViewModels receive UI events, process them, and update the ViewState accordingly. They also trigger UiActions to navigate to different screens or display notifications.

### ViewState

ViewState represents the current state of the UI. It includes data that should be displayed on the screen, such as product details, images, and other relevant information. The Presentation layer observes the ViewState and updates the UI accordingly. When the ViewState changes, Jetpack Compose recomposes the UI to reflect the new state.

## Screens

### Product Description Screen

The main feature of this app is the product description screen. It displays detailed information about a product and provides the following functionalities:

- Product Images Carousel
- "View in your space" button to launch the camera for virtual try-on
- Product title, color, and description
- Price and "Add to Cart" button

### View In Your Space Screen

The "View In Your Space" screen is a pivotal feature of this app, offering users the ability to place 3D models in their real-world environment using augmented reality. This screen showcases the app's cutting-edge capabilities with ARCore and SceneView.

This screen provides a rich AR experience and includes the following features:

- Augmented reality rendering of 3D models in real-world settings.
- Interactive gestures for placing and rotating 3D models.
- UI feedback based on the AR experience, such as readiness to place the model and hints for user actions.
- Integration with ARCore to track surfaces and enable accurate model placement.

The "View In Your Space" screen exemplifies how modern Android development can seamlessly incorporate augmented reality to enhance user engagement and interaction. It leverages the power of AR technology to create immersive and dynamic experiences within the app.

Feel free to explore and customize this screen to suit your specific AR application needs.

## Dependencies

This app uses Jetpack Compose for UI rendering and follows the principles of clean architecture for maintainability and scalability. It also leverages SceneView for 3D model placement in the virtual try-on feature.

## Get Started

To run the app, follow these steps:

1. Clone the repository.
2. Build and run the app on your Android device or emulator.

