package de.autopolish.nativeapp;
/** Keeps an unchecked 'stay signed in' login valid for this running process only. */
final class SessionState { static volatile boolean active; private SessionState(){} }
