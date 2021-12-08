package com.google.ar.sceneform;

import androidx.annotation.Nullable;

import com.google.ar.core.AugmentedFace;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collection of Trackables extensions
 */
public class Trackables {

    /**
     * Retrieve a filtered collection containing Planes
     *
     * @param trackables the all or updated trackables retrieved from
     *                   {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                   or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     */
    public static <T extends Trackable> Collection<Plane> getPlanes(Collection<T> trackables) {
        return getPlanes(trackables, (TrackingState[]) null);
    }

    /**
     * Retrieve a filtered collection containing the Planes with the specified TrackingStates.
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingStates the trackable tracking states or null for no states filter
     */
    public static <T extends Trackable> Collection<Plane> getPlanes(Collection<T> trackables
            , @Nullable TrackingState... trackingStates) {
        return filterPlanes(trackables.stream(), trackingStates)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a filtered stream containing the Planes with the specified TrackingStates.
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingStates the trackable tracking states or null for no states filter
     */
    public static <T extends Trackable> Stream<Plane> filterPlanes(Stream<T> trackables
            , @Nullable TrackingState... trackingStates) {
        return filterTrackables(trackables, Plane.class, trackingStates);
    }

    /**
     * Retrieve a filtered collection containing Augmented Images
     *
     * @param trackables the all or updated trackables retrieved from
     *                   {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                   or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     */
    public static <T extends Trackable> Collection<AugmentedImage> getAugmentedImages(Collection<T> trackables) {
        return getAugmentedImages(trackables, null, null);
    }

    /**
     * Retrieve a filtered collection containing the Augmented Images with the specified
     * TrackingState and TrackingMethod
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingState  the trackable tracking state or null for no states filter
     * @param trackingMethod the trackable tracking method or null for no tracking method filter
     */
    public static <T extends Trackable> Collection<AugmentedImage> getAugmentedImages(Collection<T> trackables
            , @Nullable TrackingState trackingState, @Nullable AugmentedImage.TrackingMethod trackingMethod) {
        return filterAugmentedImages(trackables.stream(), trackingState, trackingMethod)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a filtered stream containing the Augmented Images with the specified
     * TrackingState and TrackingMethod
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingState  the trackable tracking state or null for no states filter
     * @param trackingMethod the trackable tracking method or null for no tracking method filter
     */
    public static <T extends Trackable> Stream<AugmentedImage> filterAugmentedImages(Stream<T> trackables
            , @Nullable TrackingState trackingState, @Nullable AugmentedImage.TrackingMethod trackingMethod) {
        return filterTrackables(trackables, AugmentedImage.class, trackingState)
                .filter(augmentedImage -> trackingMethod == null || augmentedImage.getTrackingMethod() == trackingMethod);
    }

    /**
     * Retrieve a filtered collection containing Augmented Faces
     *
     * @param trackables the all or updated trackables retrieved from
     *                   {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                   or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     */
    public static <T extends Trackable> Collection<AugmentedFace> getAugmentedFaces(Collection<T> trackables) {
        return getAugmentedFaces(trackables, null);
    }

    /**
     * Retrieve a filtered collection containing the Augmented Faces with the specified
     * TrackingState and TrackingMethod
     *
     * @param trackables    the all or updated trackables retrieved from
     *                      {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                      or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingState the trackable tracking state or null for no states filter
     */
    public static <T extends Trackable> Collection<AugmentedFace> getAugmentedFaces(Collection<T> trackables
            , @Nullable TrackingState trackingState) {
        return filterAugmentedFaces(trackables.stream(), trackingState)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve a filtered stream containing the Augmented Faces with the specified
     * TrackingState and TrackingMethod
     *
     * @param trackables    the all or updated trackables retrieved from
     *                      {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                      or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingState the trackable tracking state or null for no states filter
     */
    public static <T extends Trackable> Stream<AugmentedFace> filterAugmentedFaces(Stream<T> trackables
            , @Nullable TrackingState trackingState) {
        return filterTrackables(trackables, AugmentedFace.class, trackingState);
    }

    /**
     * Retrieve a filtered collection containing the trackables with the specified type class and
     * the specified trackingStates.
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingStates the trackable tracking states or null for no states filter
     */
    public static <T extends Trackable, U extends Trackable> Stream<U> filterTrackables(Stream<T> trackables
            , Class<U> type, @Nullable TrackingState... trackingStates) {
        return filterTrackables(trackables, trackingStates)
                .filter(trackable -> trackable.getClass() == type)
                .map(type::cast);
    }

    /**
     * Retrieve a filtered collection containing the trackables with the specified trackingStates.
     *
     * @param trackables     the all or updated trackables retrieved from
     *                       {@link com.google.ar.core.Session#getAllTrackables(Class)}
     *                       or {@link Frame#getUpdatedTrackables(Class)} depending on your needs.
     * @param trackingStates the trackable tracking states or null for no states filter
     */
    public static <T extends Trackable> Stream<T> filterTrackables(Stream<T> trackables, @Nullable TrackingState... trackingStates) {
        return trackables.filter(trackable -> {
            if (trackingStates == null) {
                return true;
            }
            for (TrackingState trackingState : trackingStates) {
                if (trackingState == null || trackable.getTrackingState() == trackingState) {
                    return true;
                }
            }
            return false;
        });
    }
}