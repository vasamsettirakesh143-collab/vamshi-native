import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final ArrayList<ChatMessage> messages;

    public ChatAdapter(ArrayList<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_item, parent, false);

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {

        ChatMessage message = messages.get(position);

        holder.messageText.setText(message.text);

        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) holder.messageText.getLayoutParams();

        if (message.isUser) {

            // User: right side, ash bubble with red edge.
            holder.messageText.setBackgroundResource(
                    R.drawable.bubble_user);
            params.gravity = Gravity.END;

        } else {

            // Devil bot: left side, dark red glow bubble.
            holder.messageText.setBackgroundResource(
                    R.drawable.bubble_bot);
            params.gravity = Gravity.START;
        }

        holder.messageText.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        TextView messageText;

        ChatViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}
