package com.lkps.ctApp.utils.states

sealed class AuthenticationState {
    class Authenticated(val userId: String) : AuthenticationState()
    object Unauthenticated : AuthenticationState()
    object InvalidAuthentication : AuthenticationState()
}