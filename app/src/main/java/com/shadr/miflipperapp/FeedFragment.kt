package com.shadr.miflipperapp

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadr.miflipperapp.databinding.FragmentFeedBinding
import com.google.android.material.snackbar.Snackbar

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemManager: ItemManager

    private val viewModel: FlipViewModel by lazy {
        (requireActivity().application as MiFlipperApplication).mainViewModel
    }

    private val flipAdapter by lazy { FlipAdapter(mutableListOf(), itemManager) }

    private var currentSortColumn = "profit"
    private var isSortDescending = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        itemManager = ItemManager(requireContext())

        setupRecyclerView()
        setupSwipeToDelete()
        setupFilterSpinners()
        setupClearButton()
        setupClearListButton()
        setupSortControls()
        setupListMinProfitFilter()

        viewModel.flips.observe(viewLifecycleOwner) { currentFlipList ->
            applyFilters(currentFlipList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSwipeToDelete() {
        val context = requireContext()

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val swipedFlip = flipAdapter.getFlipAt(position)
                val itemName = itemManager.getDisplayName(swipedFlip.itemName)


                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.removeFlip(swipedFlip)
                    Snackbar.make(binding.root, context.getString(R.string.msg_discarded, itemName), Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo) {
                            viewModel.addFlip(swipedFlip)
                        }
                        .show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    viewModel.addToWatchlist(swipedFlip)
                    flipAdapter.notifyItemChanged(position)

                    Snackbar.make(binding.root,
                        context.getString(R.string.msg_added_watchlist, itemName),
                        Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewFlips)
    }

    private fun setupFilterSpinners() {
        val context = requireContext()
        val filterAll = context.getString(R.string.filter_option_all)

        val cities = arrayOf(
            filterAll,
            context.getString(R.string.city_thetford),
            context.getString(R.string.city_lymhurst),
            context.getString(R.string.city_bridgewatch),
            context.getString(R.string.city_caerleon),
            context.getString(R.string.city_martlock),
            context.getString(R.string.city_fort_sterling),
            context.getString(R.string.city_brecilien)
        )
        val cityAdapter = ArrayAdapter(context, R.layout.spinner_item, cities)
        cityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.cityFilterSpinner.adapter = cityAdapter

        val qualities = arrayOf(
            filterAll,
            context.getString(R.string.quality_normal),
            context.getString(R.string.quality_good),
            context.getString(R.string.quality_outstanding),
            context.getString(R.string.quality_excellent),
            context.getString(R.string.quality_masterpiece)
        )
        val qualityAdapter = ArrayAdapter(context, R.layout.spinner_item, qualities)
        qualityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.qualityFilterSpinner.adapter = qualityAdapter

        val tiers = arrayOf(filterAll, "4", "5", "6", "7", "8")
        val tierAdapter = ArrayAdapter(context, R.layout.spinner_item, tiers)
        tierAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.tierFilterSpinner.adapter = tierAdapter

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.flips.value?.let { applyFilters(it) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.cityFilterSpinner.onItemSelectedListener = filterListener
        binding.qualityFilterSpinner.onItemSelectedListener = filterListener
        binding.tierFilterSpinner.onItemSelectedListener = filterListener
    }

    private fun setupSortControls() {
        val context = requireContext()
        val sortOptions = arrayOf(
            context.getString(R.string.sort_profit),
            context.getString(R.string.sort_buy_price),
            context.getString(R.string.sort_sell_price),
            context.getString(R.string.sort_profit_percentage),
            context.getString(R.string.sort_time)
        )
        val sortAdapter = ArrayAdapter(context, R.layout.spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.sortSpinner.adapter = sortAdapter

        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentSortColumn = when (position) {
                    0 -> "profit"; 1 -> "buyPrice"; 2 -> "sellPrice"; 3 -> "percentage"; 4 -> "time"
                    else -> "profit"
                }
                viewModel.flips.value?.let { applyFilters(it) }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.sortDirectionButton.setOnClickListener {
            isSortDescending = !isSortDescending
            val iconRes = if (isSortDescending) R.drawable.ic_arrow_downward else R.drawable.ic_arrow_upward
            binding.sortDirectionButton.setImageResource(iconRes)
            viewModel.flips.value?.let { applyFilters(it) }
        }
    }

    private fun createQualityMap(context: Context, filterAllString: String): Map<String, String> {
        return mapOf(
            context.getString(R.string.quality_normal) to "1",
            context.getString(R.string.quality_good) to "2",
            context.getString(R.string.quality_outstanding) to "3",
            context.getString(R.string.quality_excellent) to "4",
            context.getString(R.string.quality_masterpiece) to "5",
            filterAllString to filterAllString
        )
    }

    private fun createCityMap(context: Context, filterAllString: String): Map<String, String> {

        return mapOf(
            context.getString(R.string.city_thetford) to "Thetford",
            context.getString(R.string.city_lymhurst) to "Lymhurst",
            context.getString(R.string.city_bridgewatch) to "Bridgewatch",
            context.getString(R.string.city_caerleon) to "Caerleon",
            context.getString(R.string.city_martlock) to "Martlock",
            context.getString(R.string.city_fort_sterling) to "Fort Sterling",
            context.getString(R.string.city_brecilien) to "Brecilien",
            filterAllString to filterAllString
        )
    }


    private fun applyFilters(masterFlipList: List<FlipOpportunity>) {
        if (_binding == null) return

        val context = requireContext()
        val filterAllString = context.getString(R.string.filter_option_all)

        val selectedCity = binding.cityFilterSpinner.selectedItem?.toString() ?: filterAllString
        val selectedQuality = binding.qualityFilterSpinner.selectedItem?.toString() ?: filterAllString
        val selectedTier = binding.tierFilterSpinner.selectedItem?.toString() ?: filterAllString
        val minProfitText = binding.minProfitEdittext.text.toString()
        val minProfit = minProfitText.toIntOrNull() ?: 0

        val qualityMap = createQualityMap(context, filterAllString)
        val selectedQualityNumber = qualityMap[selectedQuality]

        val cityMap = createCityMap(context, filterAllString)
        val selectedCityInternal = cityMap[selectedCity] ?: filterAllString

        val filteredList = masterFlipList.filter { flip ->

            val cityMatch = selectedCityInternal == filterAllString || flip.buyCity == selectedCityInternal

            val qualityMatch = selectedQualityNumber == filterAllString || flip.quality == selectedQualityNumber

            val tierMatch = selectedTier == filterAllString || flip.tier.toString() == selectedTier
            val profitMatch = flip.profit >= minProfit
            cityMatch && qualityMatch && tierMatch && profitMatch
        }

        val sortedList = when (currentSortColumn) {
            "buyPrice" -> if (isSortDescending) filteredList.sortedByDescending { it.buyPrice } else filteredList.sortedBy { it.buyPrice }
            "sellPrice" -> if (isSortDescending) filteredList.sortedByDescending { it.sellPrice } else filteredList.sortedBy { it.sellPrice }
            "percentage" -> if (isSortDescending) filteredList.sortedByDescending { it.profitPercentage } else filteredList.sortedBy { it.profitPercentage }
            "time" -> if (isSortDescending) filteredList.sortedByDescending { it.timestamp } else filteredList.sortedBy { it.timestamp }
            else -> if (isSortDescending) filteredList.sortedByDescending { it.profit } else filteredList.sortedBy { it.profit }
        }

        flipAdapter.updateFlips(sortedList)
    }

    private fun setupListMinProfitFilter() {
        binding.minProfitEdittext.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.flips.value?.let { applyFilters(it) }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.minProfitEdittext.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus) {
                val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewFlips.adapter = flipAdapter
        binding.recyclerViewFlips.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupClearButton() {
        binding.clearFiltersButton.setOnClickListener {
            binding.cityFilterSpinner.setSelection(0)
            binding.qualityFilterSpinner.setSelection(0)
            binding.tierFilterSpinner.setSelection(0)
            binding.minProfitEdittext.setText("0")
            Snackbar.make(binding.root, getString(R.string.msg_filters_cleared), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupClearListButton() {
        binding.clearListButton.setOnClickListener {
            viewModel.clearFlipList()
            Snackbar.make(binding.root, getString(R.string.msg_feed_cleared), Snackbar.LENGTH_SHORT).show()
        }
    }
}