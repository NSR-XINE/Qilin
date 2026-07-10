package com.mydonghua.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mydonghua.app.data.db.AppDatabase
import com.mydonghua.app.domain.model.CountdownCalculator

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_UPDATE == intent.action) {
            val title = intent.getStringExtra(KEY_TITLE)
            val countdown = intent.getStringExtra(KEY_COUNTDOWN)
            val episode = intent.getStringExtra(KEY_EPISODE)
            if (title != null && countdown != null) {
                pushWidgetData(context, title, countdown, episode ?: "")
            }
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val PREFS_NAME = "widget_data"
        private const val KEY_TITLE = "widget_title"
        private const val KEY_COUNTDOWN = "widget_countdown"
        private const val KEY_EPISODE = "widget_episode"
        const val ACTION_UPDATE = "com.mydonghua.app.UPDATE_WIDGET"

        @JvmStatic
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.countdown_widget_layout)

            try {
                kotlinx.coroutines.runBlocking {
                    val db = AppDatabase.getDatabase(context)
                    val shows = db.showDao().getAllShowsList().map { it.toDomain() }
                    val nextUp = CountdownCalculator.getNextUpShow(shows)

                    if (nextUp != null) {
                        val targetMs = CountdownCalculator.getNextReleaseDateMs(nextUp.releaseDay, nextUp.releaseTime)
                        val remaining = CountdownCalculator.calculateTimeRemaining(targetMs)
                        val countdownStr = if (remaining.airingNow) {
                            "AIRING NOW"
                        } else {
                            "${remaining.days}d ${remaining.hours}h ${remaining.minutes}m"
                        }
                        val epStr = "Episode ${nextUp.currentEp + 1} releasing ${nextUp.releaseDay}"

                        views.setTextViewText(R.id.widget_show_title, nextUp.title)
                        views.setTextViewText(R.id.widget_countdown, countdownStr)
                        views.setTextViewText(R.id.widget_episode, epStr)
                    } else {
                        views.setTextViewText(R.id.widget_show_title, "No shows yet")
                        views.setTextViewText(R.id.widget_countdown, "--d --h --m")
                        views.setTextViewText(R.id.widget_episode, "Add a show to get started")
                    }
                }
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_show_title, "No shows yet")
                views.setTextViewText(R.id.widget_countdown, "--d --h --m")
                views.setTextViewText(R.id.widget_episode, "Add a show to get started")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        @JvmStatic
        fun pushWidgetData(context: Context, title: String, countdown: String, episode: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, CountdownWidget::class.java))

            for (id in ids) {
                editor.putString("${KEY_TITLE}_$id", title)
                editor.putString("${KEY_COUNTDOWN}_$id", countdown)
                editor.putString("${KEY_EPISODE}_$id", episode)
            }
            editor.apply()

            val intent = Intent(context, CountdownWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
