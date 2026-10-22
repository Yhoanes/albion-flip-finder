package com.shadr.miflipperapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shadr.miflipperapp.databinding.FragmentWatchlistBinding
import com.google.android.material.snackbar.Snackbar
import java.text.NumberFormat
import java.util.Locale

class WatchlistFragment : Fragment() {

    private var _binding: FragmentWatchlistBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemManager: ItemManager
    private val viewModel: FlipViewModel by lazy {
        (requireActivity().application as MiFlipperApplication).mainViewModel
    }
    private val watchlistAdapter by lazy { FlipAdapter(mutableListOf(), itemManager) }

    private val numberFormatter by lazy {
        NumberFormat.getNumberInstance(Locale.getDefault())
    }

    private var masterWatchlist: List<FlipOpportunity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        itemManager = ItemManager(requireContext())

        setupRecyclerView()
        setupSwipeToRemove()
        setupClearButton()
        setupFilterSpinners()
        setupClearFiltersButton()

        viewModel.watchlistFlips.observe(viewLifecycleOwner) { flips ->
            masterWatchlist = flips
            applyFilters()
            calculateTotals(flips)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupFilterSpinners() {
        val context = requireContext()
        val filterAll = context.getString(R.string.filter_option_all)

        val tiers = arrayOf(filterAll, "4", "5", "6", "7", "8")
        val tierAdapter = ArrayAdapter(context, R.layout.spinner_item, tiers)
        tierAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.watchlistTierSpinner.adapter = tierAdapter

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
        binding.watchlistQualitySpinner.adapter = qualityAdapter

        val filterListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
        binding.watchlistTierSpinner.onItemSelectedListener = filterListener
        binding.watchlistQualitySpinner.onItemSelectedListener = filterListener
    }

    private fun setupClearFiltersButton() {
        binding.clearWatchlistFiltersButton.setOnClickListener {
            binding.watchlistTierSpinner.setSelection(0)
            binding.watchlistQualitySpinner.setSelection(0)
            Snackbar.make(binding.root, getString(R.string.msg_filters_cleared), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun createQualityMap(filterAllString: String): Map<String, String> {
        val context = requireContext()
        return mapOf(
            context.getString(R.string.quality_normal) to "1",
            context.getString(R.string.quality_good) to "2",
            context.getString(R.string.quality_outstanding) to "3",
            context.getString(R.string.quality_excellent) to "4",
            context.getString(R.string.quality_masterpiece) to "5",
            filterAllString to filterAllString
        )
    }

    private fun applyFilters() {
        if (_binding == null) return

        val filterAllString = getString(R.string.filter_option_all)
        val selectedTier = binding.watchlistTierSpinner.selectedItem?.toString() ?: filterAllString
        val selectedQuality = binding.watchlistQualitySpinner.selectedItem?.toString() ?: filterAllString

        val qualityMap = createQualityMap(filterAllString)
        val selectedQualityNumber = qualityMap[selectedQuality]

        val filteredList = masterWatchlist.filter { flip ->
            val tierMatch = selectedTier == filterAllString || flip.tier.toString() == selectedTier
            val qualityMatch = selectedQualityNumber == filterAllString || flip.quality == selectedQualityNumber

            tierMatch && qualityMatch
        }.sortedWith(compareBy<FlipOpportunity> { it.tier }.thenByDescending { it.profit })

        watchlistAdapter.updateFlips(filteredList)
    }

    private fun setupRecyclerView() {
        binding.recyclerViewWatchlist.adapter = watchlistAdapter
        binding.recyclerViewWatchlist.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSwipeToRemove() {
        val context = requireContext()

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val removedFlip = watchlistAdapter.getFlipAt(position)
                val itemName = itemManager.getDisplayName(removedFlip.itemName)


                viewModel.removeFromWatchlist(removedFlip)

                Snackbar.make(binding.root, context.getString(R.string.msg_removed_watchlist, itemName), Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo) {
                        viewModel.addToWatchlist(removedFlip)
                    }
                    .show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewWatchlist)
    }

    private fun setupClearButton() {
        binding.clearWatchlistButton.setOnClickListener {
            viewModel.clearWatchlist()
            Snackbar.make(binding.root, getString(R.string.msg_watchlist_cleared), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun calculateTotals(flips: List<FlipOpportunity>) {
        val totalInvestment = flips.sumOf { it.buyPrice.toLong() }
        val totalProfit = flips.sumOf { it.profit.toLong() }

        binding.totalInvestmentText.text = numberFormatter.format(totalInvestment)
        binding.totalProfitText.text = numberFormatter.format(totalProfit)
    }
}