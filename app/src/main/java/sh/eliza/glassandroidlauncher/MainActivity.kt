/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.eliza.glassandroidlauncher

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.glass.ui.GlassGestureDetector
import sh.eliza.glassandroidlauncher.databinding.AppItemBinding

/** Main activity of the application. It provides viewPager to move between fragments. */
class MainActivity : BaseActivity() {
  private lateinit var applications: List<AppItem>
  private var index = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.app_layout)

    applications =
      packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)
        .filter {
          packageManager.getLaunchIntentForPackage(it.packageName) != null &&
            it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM) ==
              0 &&
            it.packageName != packageName
        }
        .map {
          AppItem(
            it.packageName,
            packageManager.getApplicationLabel(it).toString(),
            packageManager.getApplicationIcon(it)
          )
        }
        .sortedBy { it.label }

    val recyclerView = findViewById<RecyclerView>(R.id.menuRecyclerView)
    val adapter = AppRecyclerViewAdapter()

    val layoutManager =
      LinearLayoutManager(
        /*context=*/ this,
        LinearLayoutManager.HORIZONTAL,
        /*reverseLayout=*/ false
      )

    recyclerView.run {
      this.layoutManager = layoutManager
      this.adapter = adapter
      isFocusable = true
    }

    val snapHelper = PagerSnapHelper().apply { attachToRecyclerView(recyclerView) }

    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
          val foundView = snapHelper.findSnapView(layoutManager) ?: return
          index = layoutManager.getPosition(foundView)
        }
      }
    )
  }

  override fun onGesture(gesture: GlassGestureDetector.Gesture): Boolean {
    return when (gesture) {
      GlassGestureDetector.Gesture.TAP -> {
        val app = applications[index]
        startActivity(packageManager.getLaunchIntentForPackage(app.packageName))
        true
      }
      else -> super.onGesture(gesture)
    }
  }

  /** Adapter for the menu horizontal recycler view. */
  private inner class AppRecyclerViewAdapter : RecyclerView.Adapter<AppViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
      return AppViewHolder(
        AppItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      )
    }

    override fun onBindViewHolder(appViewHolder: AppViewHolder, position: Int) {
      appViewHolder.bind(applications[position])
    }

    override fun getItemCount(): Int {
      return applications.size
    }
  }

  private class AppViewHolder(private val binding: AppItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: AppItem) {
      binding.run {
        this.item = item
        executePendingBindings()
      }
    }
  }
}
