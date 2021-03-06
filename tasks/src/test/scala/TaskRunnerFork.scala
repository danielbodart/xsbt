import sbt._

import org.scalacheck._
import Prop._
import Task._
import TaskGen._
import math.abs

object TaskRunnerForkTest extends Properties("TaskRunner Fork")
{
	property("fork m tasks and wait for all to complete") = forAll(MaxTasksGen, MaxWorkersGen) { (m: Int, workers: Int) =>
		val values = (0 until m).toList
		checkResult(tryRun(values.fork(f => () ).join.map(_.toList),false, workers), values)
		true
	}
	property("Fork and reduce 2") = forAll(MaxTasksGen, MaxWorkersGen) { (m: Int, workers: Int) =>
		(m > 1) ==> {
			val task = (0 to m) fork {_ * 10} reduce{_ + _}
			checkResult(tryRun(task, false, workers), 5*(m+1)*m)
		}
	}
	property("Double join") = forAll(MaxJoinGen, MaxJoinGen, MaxWorkersGen) { (a: Int, b: Int, workers: Int) =>
		runDoubleJoin(abs(a),abs(b),workers)
		true
	}
	def runDoubleJoin(a: Int, b: Int, workers: Int)
	{
		def inner(i: Int) = List.range(0, b).map(j => pure(j.toString, j)).join
		tryRun( List.range(0,a).map(inner).join, false, workers)
	}
	property("fork and reduce") = forAll(TaskListGen, MaxWorkersGen) { (m: List[Int], workers: Int) =>
		(!m.isEmpty) ==> {
			val expected = m.reduceLeft(_+_)
			checkResult(tryRun( m.reduce(_ + _), false, workers), expected)
		}
	}
}
