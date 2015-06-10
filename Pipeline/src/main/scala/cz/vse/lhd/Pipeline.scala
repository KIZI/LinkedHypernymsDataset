package cz.vse.lhd

import java.io.File

import cz.vse.lhd.core.AppConf
import cz.vse.lhd.tasks._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

/**
 * Created by propan on 6. 6. 2015.
 */
object Pipeline extends AppConf {

  val logger = LoggerFactory.getLogger(getClass)

  val liftedArgs = AppConf.args.lift

  val skipped = liftedArgs(1).collect {
    case skip if skip.startsWith("-") => skip.substring(1)
  }.getOrElse("")

  val removeAll = liftedArgs.apply(2).orElse(liftedArgs.apply(1)).exists(_ == "remove-all")

  val tasks = List(
    new HypernymExtractorIndexTask with TaskId with FileTaskCompleted {
      val id: Char = 'x'
      val taskGroup: String = "hypernym-extractor"
    },
    new HypernymExtractorIndexTask with TaskId with FileTaskCompleted {
      val id: Char = 'e'
      val taskGroup: String = "hypernym-extractor"
    },
    new OntologyCleanupIndexTask with TaskId with FileTaskCompleted {
      val id: Char = 'y'
      val taskGroup: String = "ontology-cleanup"
    },
    new OntologyCleanupTask with TaskId with FileTaskCompleted {
      val id: Char = 'c'
      val taskGroup: String = "ontology-cleanup"
    },
    new TypeInferrerIndexTask with TaskId with FileTaskCompleted {
      val id: Char = 'z'
      val taskGroup: String = "sti"
    },
    new TypeInferrerTask with TaskId with FileTaskCompleted {
      val id: Char = 'i'
      val taskGroup: String = "sti"
    },
    new FinalDatasetsMakingTask with TaskId with FileTaskCompleted {
      val id: Char = 'f'
      val taskGroup: String = "all"
    }
  ).filter(task => !skipped.contains(task.id) && !task.isCompleted)

  logger.info("Skipped tasks: " + skipped)

  val outputDir = new File(Conf.outputDir)
  if (removeAll && outputDir.isDirectory) {
    logger.info("The output directory is cleaning...")
    FileUtils.cleanDirectory()
  }

  for (task <- tasks) {
    logger.info("This task will be performed: " + getTaskName(task))
  }

  for (task <- tasks) {
    logger.info("This task is in progress: " + getTaskName(task))
    task.run()
    if (!"xyz".contains(task.id))
      task.completed(true)

  }

  def getTaskName(taskId: TaskId) = taskId.id match {
    case 'x' => "Hypernym Extraction Indexing"
    case 'e' => "Hypernym Extraction"
    case 'y' => "Ontology Cleanup Indexing"
    case 'c' => "Ontology Cleanup"
    case 'z' => "STI Indexing"
    case 'i' => "STI"
    case 'f' => "Final Datasets Making"
    case _ => "Undefined"
  }

}
