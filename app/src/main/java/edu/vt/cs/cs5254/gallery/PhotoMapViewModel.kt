package edu.vt.cs.cs5254.gallery;

import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import edu.vt.cs.cs5254.gallery.api.GalleryItem

class PhotoMapViewModel : ViewModel() {
    val galleryItemsLiveData = FlickrFetchr.galleryItemsLiveData

    val geoGalleryItemMapLiveData: LiveData<Map<String, GalleryItem>> =
            Transformations.switchMap(galleryItemsLiveData) { items ->
                val geoGalleryItemMap =
                        items.filterNot { it.latitude == "0" && it.longitude == "0" }
                                .associateBy { it.id }
                MutableLiveData<Map<String, GalleryItem>>(geoGalleryItemMap)
            }

    fun loadPhotos(reload: Boolean) {
        FlickrFetchr.fetchPhotos(reload)
    }

    fun storeThumbnail(id: String, drawable: Drawable) {
        FlickrFetchr.storeThumbnail(id, drawable)
    }
}
