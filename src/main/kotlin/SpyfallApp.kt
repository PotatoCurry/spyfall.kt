package io.github.potatocurry.spyfallkt

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.minutes
import com.soywiz.klock.seconds
import com.soywiz.klock.toTimeString
import io.github.potatocurry.spyfallkt.SpyfallState.createGame
import io.github.potatocurry.spyfallkt.SpyfallState.createUser
import io.github.potatocurry.spyfallkt.SpyfallState.games
import io.github.potatocurry.spyfallkt.SpyfallState.locations
import io.github.potatocurry.spyfallkt.SpyfallState.users
import io.github.potatocurry.spyfallkt.SpyfallState.usersByGame
import io.kweb.Kweb
import io.kweb.dom.element.creation.ElementCreator
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.new
import io.kweb.dom.title
import io.kweb.plugins.fomanticUI.fomantic
import io.kweb.plugins.fomanticUI.fomanticUIPlugin
import io.kweb.routing.route
import io.kweb.state.KVar
import io.kweb.state.path
import io.kweb.state.render.renderEach
import io.kweb.state.render.toVar
import io.kweb.state.simpleUrlParser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.timer

fun main() {
    val port = System.getenv("PORT").toInt()
    Kweb(port, plugins = listOf(fomanticUIPlugin)) {
        doc.head.new {
            title().text("Spyfall")
        }
        doc.body.new {
            div(fomantic.ui.center.aligned.very.padded.basic.container.segment).new {
                div(fomantic.ui.vertical).new {
                    div(fomantic.ui.icon.header).new {
                        i().classes("huge spy icon")
                        h1().text("Spyfall")
                    }
                }
                div(fomantic.ui.vertical).new {
                    val path = url(simpleUrlParser).path
                    route {
                        path("/") {
                            div(fomantic.ui.huge.vertical.buttons).new {
                                button(fomantic.ui.button).apply {
                                    text("Create Game")
                                    on.click {
                                        path.value = "/create"
                                    }
                                }
                                button(fomantic.ui.button).apply {
                                    text("Join Game")
                                    on.click {
                                        path.value = "/join"
                                    }
                                }
                            }
                        }
                        path("/create") {
                            div(fomantic.ui.action.input).new {
                                val titleInput = input(InputType.text, placeholder = "Title")

                                val titleText = KVar("")
                                titleInput.value = titleText
                                button(fomantic.ui.button).apply {
                                    text("Create Game") // tools
                                    on.click {
                                        val code = createGame(titleText.value).code
                                        path.value = "/join/$code"
                                    }
                                }
                            }
                        }
                        path("/join") {
                            div(fomantic.ui.action.input).new {
                                val codeInput = input(InputType.text, placeholder = "Code")
                                val codeText = KVar("")
                                codeInput.value = codeText
                                button(fomantic.ui.button).apply {
                                    text("Join Game")
                                    on.click {
                                        val code = codeText.value
                                        path.value = "/join/$code"
                                    }
                                }
                            }
                        }
                        path("/join/{code}") { params ->
                            val code = params.getValue("code").value
                            val game = toVar(games, code) // TODO: Check if game exists
                            h2(fomantic.ui.header).text(game.map(SpyfallState.Game::title))
                            if (game.value.state == SpyfallState.GameState.INACTIVE) {
                                val state = game.map(SpyfallState.Game::state)
                                val joinDiv = div(fomantic.ui.action.input)
                                val inProgressElement = h3()
                                joinDiv.new {
                                    val nameInput = input(InputType.text, placeholder = "Username")
                                    val nameText = KVar("")
                                    nameInput.value = nameText
                                    button(fomantic.ui.button).apply {
                                        text("Join Game")
                                        on.click {
                                            val name = nameText.value
                                            val user = createUser(name, code)
                                            state.addListener { _, _ ->
                                                path.value = "/game/$code/${user.uid}"
                                            }
                                        }
                                    }
                                }
                                val startButton = button(fomantic.ui.primary.button).apply {
                                    text("Start Game") // gamepad
                                    on.click {
                                        game.value = game.value.copy(state = SpyfallState.GameState.ACTIVE)
                                    }
                                }
                                state.addListener { _, _ ->
                                    joinDiv.delete()
                                    startButton.delete()
                                    inProgressElement.text("Game in progress")
                                }
                            } else {
                                h3().text("Game in progress")
                            }
                            div(fomantic.ui.hidden.divider)
                            renderUserList(game.value)
                        }
                        path("/game/{code}/{user}") { params ->
                            val code = params.getValue("code").value
                            val userUid = params.getValue("user").value
                            val game = toVar(games, code)
                            val user = users[userUid]
                            if (user == null) {
                                p().text("User not found")
                            } else {
                                val spy = user == game.value.spy
                                if (spy)
                                    user.role = "Spy"
                                else
                                    user.role = game.value.nextRole()
                                val timer = KVar(10.minutes)

                                p().text(timer.map { it.toTimeString(2) })
                                div(fomantic.ui /* TODO: Add fade reveal classes */).new {

                                }
                                p().text(user.name)
                                val roleElement = p().text(user.role) // TODO: Conceal when not hovered over
                                val locationElement = if (spy)
                                    p().text("Unknown")
                                else
                                    p().text(game.value.location.name)
                                renderUserList(game.value)
                                div(fomantic.ui.grid).new {
                                    locations.forEach { location ->
                                        div(fomantic.four.wide.column).new {
                                            i().classes("globe", "icon")
                                            div(fomantic.content).text(location.name)
                                        }
                                    }
                                }
                                GlobalScope.launch {
                                    timer(period = 1000) {
                                        if (timer.value != TimeSpan.ZERO) {
                                            timer.value -= 1.seconds
                                        } else {
                                            game.value = game.value.copy(state = SpyfallState.GameState.FINISHED)
                                            cancel()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ElementCreator<*>.renderUserList(game: SpyfallState.Game) {
    val users = usersByGame(game)
    div(fomantic.ui.centered.grid).new {
        div(fomantic.four.wide.column).new {
            div(fomantic.ui.divided.list).new {
                renderEach(users) { user ->
                    div(fomantic.ui.item).new {
                        i(fomantic.user.icon)
                        div(fomantic.content).text(user.map(SpyfallState.User::name))
                    }
                }
            }
        }
    }
}
