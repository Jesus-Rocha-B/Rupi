package pe.rupi.mobile.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.rupi.mobile.ui.state.NavDestination
import java.util.ArrayList

/**
 * ViewModel de navegación de pantalla única (back stack tipado + preservación en SavedStateHandle).
 */
class NavigationViewModel(
 private val savedStateHandle: SavedStateHandle
) : ViewModel() {

 private val _backStack = MutableStateFlow<List<NavDestination>>(
  savedStateHandle.get<ArrayList<NavDestination>>(KEY_BACK_STACK)?.toList() ?: listOf(NavDestination.Login)
 )
 val backStack: StateFlow<List<NavDestination>> = _backStack.asStateFlow()

 val current: NavDestination
  get() = _backStack.value.lastOrNull() ?: NavDestination.Login

 fun initialize(authenticated: Boolean, openNotifications: Boolean) {
  val restored = savedStateHandle.get<ArrayList<NavDestination>>(KEY_BACK_STACK)?.toList()
  if (restored != null && restored.isNotEmpty()) {
   if (authenticated && restored.first() == NavDestination.Login) {
    setStack(listOf(NavDestination.Topics))
   } else if (!authenticated && restored.last() !is NavDestination.Login && restored.last() !is NavDestination.SavedSummaries && restored.last() !is NavDestination.SavedSummaryDetail) {
    setStack(listOf(NavDestination.Login))
   } else {
    setStack(restored)
   }
   return
  }
  val initial = when {
   !authenticated -> listOf(NavDestination.Login)
   openNotifications -> listOf(NavDestination.Topics, NavDestination.Notifications)
   else -> listOf(NavDestination.Topics)
  }
  setStack(initial)
 }

 fun navigateTo(destination: NavDestination) {
  val currentList = _backStack.value
  if (currentList.lastOrNull() == destination) return
  setStack(currentList + destination)
 }

 fun replaceTop(destination: NavDestination) {
  val currentList = _backStack.value
  if (currentList.isEmpty()) {
   setStack(listOf(destination))
  } else {
   setStack(currentList.dropLast(1) + destination)
  }
 }

 fun resetTo(root: NavDestination) {
  setStack(listOf(root))
 }

 fun resetToStack(stack: List<NavDestination>) {
  require(stack.isNotEmpty())
  setStack(stack)
 }

 fun navigateUp(): Boolean {
  val currentList = _backStack.value
  if (currentList.size <= 1) return false
  setStack(currentList.dropLast(1))
  return true
 }

 fun popTo(predicate: (NavDestination) -> Boolean): Boolean {
  val currentList = _backStack.value
  val index = currentList.indexOfLast(predicate)
  if (index < 0) return false
  setStack(currentList.subList(0, index + 1))
  return true
 }

 private fun setStack(stack: List<NavDestination>) {
  _backStack.value = stack
  savedStateHandle[KEY_BACK_STACK] = ArrayList(stack)
 }

 companion object {
  private const val KEY_BACK_STACK = "nav_back_stack"
 }
}
