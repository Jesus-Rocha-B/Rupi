package pe.rupi.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.rupi.mobile.data.model.LearningRouteSummary
import pe.rupi.mobile.data.model.RouteNode
import pe.rupi.mobile.data.model.RouteNodeState
import pe.rupi.mobile.ui.state.NavDestination
import pe.rupi.mobile.ui.viewmodel.NavigationViewModel

class NavigationViewModelTest {

	@Test
	fun `initialize desautenticado navega a Login`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = false, openNotifications = false)

		assertEquals(listOf(NavDestination.Login), vm.backStack.value)
		assertEquals(NavDestination.Login, vm.current)
	}

	@Test
	fun `initialize autenticado sin openNotifications navega a Topics`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = false)

		assertEquals(listOf(NavDestination.Topics), vm.backStack.value)
		assertEquals(NavDestination.Topics, vm.current)
	}

	@Test
	fun `initialize autenticado con openNotifications apila Topics y Notifications`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = true)

		assertEquals(
			listOf(NavDestination.Topics, NavDestination.Notifications),
			vm.backStack.value
		)
		assertEquals(NavDestination.Notifications, vm.current)
	}

	@Test
	fun `navigateTo agrega destino a la pila sin duplicar el tope identico`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = false)

		val route = LearningRouteSummary("r-1", "Matemática")
		vm.navigateTo(NavDestination.RouteDetail(route))
		assertEquals(2, vm.backStack.value.size)
		assertEquals(NavDestination.RouteDetail(route), vm.current)

		// Reintentar navegar al mismo destino no debe duplicar el tope
		vm.navigateTo(NavDestination.RouteDetail(route))
		assertEquals(2, vm.backStack.value.size)
	}

	@Test
	fun `navigateUp desapila correctamente y retorna false si queda un solo elemento`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = false)

		vm.navigateTo(NavDestination.Achievements)
		assertEquals(NavDestination.Achievements, vm.current)

		val popped = vm.navigateUp()
		assertTrue(popped)
		assertEquals(NavDestination.Topics, vm.current)

		val poppedRoot = vm.navigateUp()
		assertFalse(poppedRoot)
		assertEquals(NavDestination.Topics, vm.current)
	}

	@Test
	fun `replaceTop reemplaza el ultimo elemento manteniendo la base`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = false)

		val route = LearningRouteSummary("r-1", "Comunicación")
		val node1 = RouteNode("n-1", "Lectura", 1, RouteNodeState.DISPONIBLE)
		val node2 = RouteNode("n-2", "Vocabulario", 2, RouteNodeState.DISPONIBLE)

		vm.navigateTo(NavDestination.Lesson(route, node1))
		assertEquals(NavDestination.Lesson(route, node1), vm.current)

		vm.replaceTop(NavDestination.Lesson(route, node2))
		assertEquals(listOf(NavDestination.Topics, NavDestination.Lesson(route, node2)), vm.backStack.value)
		assertEquals(NavDestination.Lesson(route, node2), vm.current)
	}

	@Test
	fun `resetTo limpia el historial completo y establece el nuevo destino unico`() {
		val vm = NavigationViewModel()
		vm.initialize(authenticated = true, openNotifications = true)
		assertEquals(2, vm.backStack.value.size)

		vm.resetTo(NavDestination.Login)
		assertEquals(listOf(NavDestination.Login), vm.backStack.value)
		assertEquals(NavDestination.Login, vm.current)
	}
}
