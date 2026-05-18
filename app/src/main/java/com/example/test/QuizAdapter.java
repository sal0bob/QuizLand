package com.example.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.test.models.QuizResponse;

import java.util.ArrayList;
import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private final Context context;
    private final List<QuizResponse> quizzes = new ArrayList<>();
    private final List<QuizResponse> allQuizzes = new ArrayList<>();

    public QuizAdapter(Context context) {
        this.context = context;
    }

    public void setQuizzes(List<QuizResponse> newQuizzes) {
        quizzes.clear();
        allQuizzes.clear();

        if (newQuizzes != null) {
            quizzes.addAll(newQuizzes);
            allQuizzes.addAll(newQuizzes);
        }

        notifyDataSetChanged();
    }

    public void filterByTitle(String query) {
        quizzes.clear();

        if (query == null || query.trim().isEmpty()) {
            quizzes.addAll(allQuizzes);
        } else {
            String lowerQuery = query.toLowerCase().trim();

            for (QuizResponse quiz : allQuizzes) {
                String title = quiz.title != null ? quiz.title.toLowerCase() : "";

                if (title.contains(lowerQuery)) {
                    quizzes.add(quiz);
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quiz_list, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        QuizResponse quiz = quizzes.get(position);

        holder.quizTitle.setText(quiz.title != null ? quiz.title : "Без названия");

        int visits = quiz.visits != null ? quiz.visits : 0;
        int rating = quiz.rating != null ? quiz.rating : 0;

        holder.quizInfo.setText("👁 " + visits + "   ★ " + rating);

        if (quiz.previewUrl != null && !quiz.previewUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(quiz.previewUrl)
                    .centerCrop()
                    .into(holder.quizPreview);
        } else {
            holder.quizPreview.setImageResource(R.drawable.puzzle);
        }

        View.OnClickListener openQuizListener = v -> {
            SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
            boolean isLogin = prefs.getBoolean("isLogin", false);

            if (!isLogin) {
                Intent intent = new Intent(context, AuthActivity.class);
                context.startActivity(intent);
                return;
            }

            Intent intent = new Intent(context, QuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.id);
            context.startActivity(intent);
        };

        holder.itemView.setOnClickListener(openQuizListener);
        holder.startQuizButton.setOnClickListener(openQuizListener);
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    static class QuizViewHolder extends RecyclerView.ViewHolder {

        ImageView quizPreview;
        TextView quizTitle;
        TextView quizInfo;
        ImageButton startQuizButton;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);

            quizPreview = itemView.findViewById(R.id.quizPreview);
            quizTitle = itemView.findViewById(R.id.quizTitle);
            quizInfo = itemView.findViewById(R.id.quizInfo);
            startQuizButton = itemView.findViewById(R.id.startQuizButton);
        }
    }
}