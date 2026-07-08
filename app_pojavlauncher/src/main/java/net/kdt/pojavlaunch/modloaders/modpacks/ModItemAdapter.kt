package net.kdt.pojavlaunch.modloaders.modpacks

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.RecyclerView
import com.kdt.SimpleArrayAdapter
import git.artdeell.mojo.R
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModpackApi
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ImageReceiver
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchFilters
import net.kdt.pojavlaunch.modloaders.modpacks.models.SearchResult
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import java.util.Arrays
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.Future

class ModItemAdapter(resources: Resources, private val mModpackApi: ModpackApi, private val mSearchResultCallback: SearchResultCallback) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), TaskCountListener {
    private val mLoadingAdapter = SimpleArrayAdapter(Collections.singletonList("Loading"))
    private val mViewHolderSet = Collections.newSetFromMap(WeakHashMap<RecyclerView.ViewHolder, Boolean>())
    private val mIconCache = ModIconCache()
    private var mModItems = MOD_ITEMS_EMPTY
    private val mCornerDimensionCache: Float = resources.getDimension(R.dimen._1sdp) / 250f
    private var mTaskInProgress: Future<*>? = null
    private var mSearchFilters: SearchFilters? = null
    private var mCurrentResult: SearchResult? = null
    private var mLastPage = false
    private var mTasksRunning = false

    fun performSearchQuery(searchFilters: SearchFilters) {
        mTaskInProgress?.cancel(true)
        mTaskInProgress = null
        mSearchFilters = searchFilters
        mLastPage = false
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(mSearchFilters!!, null))
            .startOnExecutor(PojavApplication.sExecutorService)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(viewGroup.context)
        return when (viewType) {
            VIEW_TYPE_MOD_ITEM -> {
                val view = layoutInflater.inflate(R.layout.view_mod, viewGroup, false)
                ViewHolder(view)
            }
            VIEW_TYPE_LOADING -> {
                val view = layoutInflater.inflate(R.layout.view_loading, viewGroup, false)
                LoadingViewHolder(view)
            }
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            VIEW_TYPE_MOD_ITEM -> (holder as ViewHolder).setStateLimited(mModItems[position])
            VIEW_TYPE_LOADING -> loadMoreResults()
            else -> throw RuntimeException("Unimplemented view type!")
        }
    }

    override fun getItemCount(): Int {
        if (mLastPage || mModItems.isEmpty()) return mModItems.size
        return mModItems.size + 1
    }

    private fun loadMoreResults() {
        if (mTaskInProgress != null) return
        mTaskInProgress = SelfReferencingFuture(SearchApiTask(mSearchFilters!!, mCurrentResult))
            .startOnExecutor(PojavApplication.sExecutorService)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < mModItems.size) VIEW_TYPE_MOD_ITEM else VIEW_TYPE_LOADING
    }

    override fun onUpdateTaskCount(taskCount: Int): Boolean {
        Tools.runOnUiThread {
            mTasksRunning = taskCount != 0
            for (viewHolder in mViewHolderSet) {
                (viewHolder as ViewHolder).updateInstallButtonState()
            }
        }
        return false
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var mModDetail: ModDetail? = null
        private var mModItem: ModItem? = null
        private val mTitle: TextView = view.findViewById(R.id.mod_title_textview)
        private val mDescription: TextView = view.findViewById(R.id.mod_body_textview)
        private val mIconView: ImageView = view.findViewById(R.id.mod_thumbnail_imageview)
        private val mSourceView: ImageView = view.findViewById(R.id.mod_source_imageview)
        private var mExtendedLayout: View? = null
        private var mExtendedSpinner: Spinner? = null
        private var mExtendedButton: Button? = null
        private var mExtendedErrorTextView: TextView? = null
        private var mExtensionFuture: Future<*>? = null
        private var mThumbnailBitmap: Bitmap? = null
        private var mImageReceiver: ImageReceiver? = null
        private var mInstallEnabled = false
        private val mVersionAdapter = SimpleArrayAdapter<String>(null)

        init {
            mViewHolderSet.add(this)
            view.setOnClickListener { v ->
                if (!hasExtended()) {
                    mExtendedLayout = (v.findViewById<View>(R.id.mod_limited_state_stub) as ViewStub).inflate()
                    mExtendedButton = mExtendedLayout!!.findViewById(R.id.mod_extended_select_version_button)
                    mExtendedSpinner = mExtendedLayout!!.findViewById(R.id.mod_extended_version_spinner)
                    mExtendedErrorTextView = mExtendedLayout!!.findViewById(R.id.mod_extended_error_textview)
                    mExtendedButton!!.setOnClickListener {
                        mModpackApi.handleModpackInstallation(
                            mExtendedButton!!.context.applicationContext,
                            mModDetail,
                            mExtendedSpinner!!.selectedItemPosition
                        )
                    }
                    mExtendedSpinner!!.adapter = mLoadingAdapter
                } else {
                    if (isExtended()) closeDetailedView()
                    else openDetailedView()
                }
                if (isExtended() && mModDetail == null && mExtensionFuture == null) {
                    setDetailedStateDefault()
                    mExtensionFuture = SelfReferencingFuture { myFuture ->
                        mModDetail = mModpackApi.getModDetails(mModItem!!)
                        println(mModDetail)
                        Tools.runOnUiThread {
                            if (myFuture.isCancelled) return@runOnUiThread
                            mExtensionFuture = null
                            setStateDetailed(mModDetail)
                        }
                    }.startOnExecutor(PojavApplication.sExecutorService)
                }
            }
        }

        fun setStateLimited(item: ModItem) {
            mModDetail = null
            mThumbnailBitmap?.let {
                mIconView.setImageBitmap(null)
                it.recycle()
            }
            mImageReceiver?.let { mIconCache.cancelImage(it) }
            mExtensionFuture?.let {
                it.cancel(true)
                mExtensionFuture = null
            }
            mModItem = item
            mImageReceiver = { bm ->
                mImageReceiver = null
                mThumbnailBitmap = bm
                val drawable = RoundedBitmapDrawableFactory.create(mIconView.resources, bm)
                drawable.cornerRadius = mCornerDimensionCache * bm.height
                mIconView.setImageDrawable(drawable)
            }
            mIconCache.getImage(mImageReceiver!!, mModItem!!.getIconCacheTag(), mModItem!!.imageUrl)
            mSourceView.setImageResource(getSourceDrawable(item.apiSource))
            mTitle.text = item.title
            mDescription.text = item.description
            if (hasExtended()) closeDetailedView()
        }

        private fun setStateDetailed(detailedItem: ModDetail?) {
            if (detailedItem != null) {
                setInstallEnabled(true)
                mExtendedErrorTextView!!.visibility = View.GONE
                mVersionAdapter.setObjects(Arrays.asList(*detailedItem.versionNames))
                mExtendedSpinner!!.adapter = mVersionAdapter
            } else {
                closeDetailedView()
                setInstallEnabled(false)
                mExtendedErrorTextView!!.visibility = View.VISIBLE
                mExtendedSpinner!!.adapter = null
                mVersionAdapter.setObjects(null)
            }
        }

        private fun openDetailedView() {
            mExtendedLayout!!.visibility = View.VISIBLE
            mDescription.maxLines = 99
            val futureBottom = mDescription.bottom + Tools.mesureTextviewHeight(mDescription) - mDescription.height
            val params = mExtendedLayout!!.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom = if (futureBottom > mIconView.bottom) R.id.mod_body_textview else R.id.mod_thumbnail_imageview
            mExtendedLayout!!.layoutParams = params
        }

        private fun closeDetailedView() {
            mExtendedLayout!!.visibility = View.GONE
            mDescription.maxLines = 3
        }

        private fun setDetailedStateDefault() {
            setInstallEnabled(false)
            mExtendedSpinner!!.adapter = mLoadingAdapter
            mExtendedErrorTextView!!.visibility = View.GONE
            openDetailedView()
        }

        private fun hasExtended() = mExtendedLayout != null
        private fun isExtended() = hasExtended() && mExtendedLayout!!.visibility == View.VISIBLE

        private fun getSourceDrawable(apiSource: Int): Int {
            return when (apiSource) {
                Constants.SOURCE_CURSEFORGE -> R.drawable.ic_curseforge
                Constants.SOURCE_MODRINTH -> R.drawable.ic_modrinth
                else -> throw RuntimeException("Unknown API source")
            }
        }

        private fun setInstallEnabled(enabled: Boolean) {
            mInstallEnabled = enabled
            updateInstallButtonState()
        }

        fun updateInstallButtonState() {
            if (mExtendedButton != null) mExtendedButton!!.isEnabled = mInstallEnabled && !mTasksRunning
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class SearchApiTask(
        private val mSearchFilters: SearchFilters,
        private val mPreviousResult: SearchResult?
    ) : SelfReferencingFuture.FutureInterface {
        @SuppressLint("NotifyDataSetChanged")
        override fun run(myFuture: Future<*>) {
            var result = mModpackApi.searchMod(mSearchFilters, mPreviousResult)
            var resultModItems = result?.results
            if (!resultModItems.isNullOrEmpty() && mPreviousResult != null) {
                val newModItems = arrayOfNulls<ModItem>(resultModItems.size + mModItems.size)
                System.arraycopy(mModItems, 0, newModItems, 0, mModItems.size)
                System.arraycopy(resultModItems, 0, newModItems, mModItems.size, resultModItems.size)
                @Suppress("UNCHECKED_CAST")
                resultModItems = newModItems as Array<ModItem>
            }
            val finalModItems = resultModItems
            Tools.runOnUiThread {
                if (myFuture.isCancelled) return@runOnUiThread
                mTaskInProgress = null
                when {
                    finalModItems == null -> mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_INTERNAL)
                    finalModItems.isEmpty() -> {
                        if (mPreviousResult != null) {
                            mLastPage = true
                            notifyItemChanged(mModItems.size)
                            mSearchResultCallback.onSearchFinished()
                            return@runOnUiThread
                        }
                        mSearchResultCallback.onSearchError(SearchResultCallback.ERROR_NO_RESULTS)
                    }
                    else -> mSearchResultCallback.onSearchFinished()
                }
                mCurrentResult = result
                if (finalModItems == null) {
                    mModItems = MOD_ITEMS_EMPTY
                    notifyDataSetChanged()
                    return@runOnUiThread
                }
                if (mPreviousResult != null) {
                    val prevLength = mModItems.size
                    mModItems = finalModItems
                    notifyItemChanged(prevLength)
                    notifyItemRangeInserted(prevLength + 1, mModItems.size)
                } else {
                    mModItems = finalModItems
                    notifyDataSetChanged()
                }
            }
        }
    }

    interface SearchResultCallback {
        fun onSearchFinished()
        fun onSearchError(error: Int)

        companion object {
            const val ERROR_INTERNAL = 0
            const val ERROR_NO_RESULTS = 1
        }
    }

    companion object {
        private val MOD_ITEMS_EMPTY = arrayOf<ModItem>()
        private const val VIEW_TYPE_MOD_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
