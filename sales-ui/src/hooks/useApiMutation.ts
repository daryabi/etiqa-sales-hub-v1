import { useMutation, useQueryClient, type QueryKey } from '@tanstack/react-query';
import { useNotify } from '../notifications/notificationContext';

interface UseApiMutationOptions<TData, TVariables> {
  mutationFn: (variables: TVariables) => Promise<TData>;
  queryKeysToInvalidate?: QueryKey[];
  successMessage?: string | ((mutationResult: TData) => string);
  onSuccess?: (mutationResult: TData) => void;
}

export function useApiMutation<TData, TVariables>({
  mutationFn,
  queryKeysToInvalidate = [],
  successMessage,
  onSuccess,
}: UseApiMutationOptions<TData, TVariables>) {
  const queryClient = useQueryClient();
  const notify = useNotify();

  return useMutation({
    mutationFn,
    onSuccess: (mutationResult) => {
      for (const queryKey of queryKeysToInvalidate) {
        queryClient.invalidateQueries({ queryKey });
      }
      if (successMessage) {
        notify('success', typeof successMessage === 'function' ? successMessage(mutationResult) : successMessage);
      }
      onSuccess?.(mutationResult);
    },
    onError: (error: Error) => {
      notify('error', error.message);
    },
  });
}
