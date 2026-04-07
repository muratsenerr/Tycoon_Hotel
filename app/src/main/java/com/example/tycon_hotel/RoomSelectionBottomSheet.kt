package com.example.tycon_hotel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RoomSelectionBottomSheet(
    private val costs: Map<RoomType, Double>,
    private val hotelRoomCount: Int,
    private val onOptionSelected: (BuildOption) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_room_selection_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvOptions = view.findViewById<RecyclerView>(R.id.rvBuildOptions)
        
        // Dinamik Fiyatlar (MainActivity'den gelen)
        val options = listOf(
            BuildOption(RoomType.HOTEL_ROOM, "Otel Odası", costs[RoomType.HOTEL_ROOM] ?: 50.0, 2.0, android.R.drawable.ic_menu_add),
            BuildOption(RoomType.RESTAURANT, "Restoran", costs[RoomType.RESTAURANT] ?: 750.0, 15.0, android.R.drawable.ic_menu_today),
            BuildOption(RoomType.SPA, "Spa", costs[RoomType.SPA] ?: 4000.0, 60.0, android.R.drawable.ic_menu_view),
            BuildOption(RoomType.BAR, "Bar", costs[RoomType.BAR] ?: 10000.0, 150.0, android.R.drawable.ic_menu_day),
            BuildOption(RoomType.LAUNDRY, "Çamaşırhane", costs[RoomType.LAUNDRY] ?: 12000.0, 200.0, android.R.drawable.ic_menu_save, isLocked = hotelRoomCount < 5)
        )

        rvOptions.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvOptions.adapter = BuildOptionAdapter(options) { selectedOption ->
            onOptionSelected(selectedOption)
            dismiss()
        }
    }
}
