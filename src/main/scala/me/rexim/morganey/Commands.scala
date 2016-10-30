package me.rexim.morganey

import me.rexim.morganey.interpreter.{ReplContext, ReplResult}
import me.rexim.morganey.reduction.Computation
import me.rexim.morganey.syntax.LambdaParser
import me.rexim.morganey.util._

import scala.util.{Failure, Success}

object Commands {

  type CommandTask = ReplContext => Computation[ReplResult[String]]

  case class Command(
    name: String,
    task: String => ReplContext => ReplResult[String],
    hasTermArgument: Boolean)

  private def command(f: ReplContext => ReplResult[String]): CommandTask =
    { context: ReplContext => Computation(f(context)) }

  private def unknownCommand(command: String): CommandTask =
    { context: ReplContext => Computation.failed(new RuntimeException(s"Unknown command '$command'!")) }

  val commandPattern  = ":([a-zA-Z]*)".r
  val commandWithArgs = ":([a-zA-Z]*) (.*)".r
  val unbindCountThreshold = 10

  val commands =
    Seq(
      Command("exit",   exitREPL,       false),
      Command("raw",    rawPrintTerm,   false),
      Command("unbind", unbindBindings, true)
    )
    .map(x => x.name -> x)
    .toMap

  def parseCommand(line: String): Option[(String, String)] =
    line match {
      case commandPattern(cmd)        => Some((cmd, ""))
      case commandWithArgs(cmd, args) => Some((cmd, args))
      case _                          => None
    }

  def unapply(line: String): Option[CommandTask] =
    parseCommand(line) map {
      case (cmd, args) if commands contains cmd => command(commands(cmd).task(args.trim))
      case (cmd, _)                             => unknownCommand(cmd)
    }

  private def exitREPL(args: String)(context: ReplContext): ReplResult[String] =
    sys.exit(0)

  private def rawPrintTerm(args: String)(context: ReplContext): ReplResult[String] = {
    val parseResult = LambdaParser.parseWith(args, _.term)
    val output = parseResult match {
      case Success(term) => term.toString
      case Failure(e)    => e.getMessage
    }
    ReplResult(context, Option(output))
  }

  private def unbindBindings(args: String)(context: ReplContext): ReplResult[String] =
    validRegex(args) match {
      case None =>
        ReplResult(context, Some(s"'$args' is not a valid regular expression!"))
      case Some(matcher) =>
        val (newContext, removedBindings) = context.removeBindings(b => !matcher(b.variable.name))

        val removed = removedBindings.map(_.variable.name).distinct
        val message = removed match {
          case Nil       => "No bindings were removed!"
          case hd :: Nil => s"Binding '$hd' was removed!"
          case bindings if bindings.size > unbindCountThreshold =>
            s"${bindings.size} bindings were removed!"
          case in :+ ls  =>
            val bindingEnumeration = in.map(b => s"'$b'").mkString(", ") + s", and '$ls'"
            s"Bindings $bindingEnumeration were removed!"
        }

        ReplResult(newContext, Option(message))
    }

}
