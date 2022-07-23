package edu.vt.cs.cs5254.gallery

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import edu.vt.cs.cs5254.gallery.api.GalleryItem

class PhotoMapFragment : MapViewFragment(), GoogleMap.OnMarkerClickListener {

    private lateinit var photoMapViewModel: PhotoMapViewModel
    private lateinit var thumbnailDownloader: ThumbnailDownloader<Marker>

    private var geoGalleryItemMap = emptyMap<String, GalleryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        retainInstance = true

        photoMapViewModel =
                ViewModelProvider(this).get(PhotoMapViewModel::class.java)

        val responseHandler = Handler(Looper.myLooper()!!)
        thumbnailDownloader =
                ThumbnailDownloader(responseHandler) { marker, bitmap ->
                    setMarkerIcon(marker, bitmap)
                    val drawable = BitmapDrawable(resources, bitmap)
                    photoMapViewModel.storeThumbnail(marker.tag as String, drawable)
                }

        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.reload_button -> {
                photoMapViewModel.loadPhotos(true)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateMapView(inflater, container, savedInstanceState, R.layout.fragment_photo_map, R.id.map_view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onMapViewCreated(view, savedInstanceState) { googleMap ->
            googleMap.setOnMarkerClickListener(this@PhotoMapFragment)
            photoMapViewModel.geoGalleryItemMapLiveData.observe(
                viewLifecycleOwner,
                Observer { galleryItemMap ->
                geoGalleryItemMap = galleryItemMap
                updateUI()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(
                thumbnailDownloader.viewLifecycleObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(
                thumbnailDownloader.fragmentLifecycleObserver
        )
    }

    private fun updateUI() {

        // if the fragment is not currently added to its activity, or
        // if there are not gallery items, do not update the UI
        if (!isAdded || geoGalleryItemMap.isEmpty()) {
            return
        }

        if (!super.mapIsInitialized()) return

        Log.i(TAG, "Gallery has has " + geoGalleryItemMap.size + " items")

        // remove all markers, overlays, etc. from the map
        googleMap.clear()

        val bounds = LatLngBounds.Builder()
        for (item in geoGalleryItemMap.values) {
            // log the information of each gallery item with a valid lat-lng
            Log.i(
                    TAG,
                    "Item id=${item.id} " +
                            "lat=${item.latitude} long=${item.longitude} " +
                            "title=${item.title}"
            )
            // create a lan-lng point for the item and add it to the lat-lng bounds
            val itemPoint = LatLng(item.latitude.toDouble(), item.longitude.toDouble())
            bounds.include(itemPoint)

            // create a marker for the item and add it to the map
            val itemMarker = MarkerOptions().position(itemPoint).title(item.title)
            val marker = googleMap.addMarker(itemMarker)

            if(item.drawable == null) {
                thumbnailDownloader.queueThumbnail(marker, item.url)
            }
            else {
                setMarkerIcon(marker, PictureUtil.drawableToBitmap(item.drawable!!))
            }

            marker.tag = item.id
        }

        Log.i(TAG, "Expecting ${geoGalleryItemMap.size} markers on the map")
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val galleryItemId = marker?.tag as String
        Log.i(TAG, "Clicked on marker $galleryItemId")
        val item = geoGalleryItemMap[galleryItemId]
        val uri = item?.photoPageUri ?: return false
        val intent = PhotoPageActivity.newIntent(requireContext(), uri)
        startActivity(intent)
        return true
    }

    companion object {
        private const val TAG = "PhotoMapFragment"
    }
}